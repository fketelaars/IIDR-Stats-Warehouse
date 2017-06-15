/****************************************************************************
 ** Licensed Materials - Property of IBM 
 ** IBM InfoSphere Change Data Capture
 ** 5724-U70
 ** 
 ** (c) Copyright IBM Corp. 2001, 2016 All rights reserved.
 ** 
 ** The following sample of source code ("Sample") is owned by International 
 ** Business Machines Corporation or one of its subsidiaries ("IBM") and is 
 ** copyrighted and licensed, not sold. You may use, copy, modify, and 
 ** distribute the Sample for your own use in any form without payment to IBM.
 ** 
 ** The Sample code is provided to you on an "AS IS" basis, without warranty of 
 ** any kind. IBM HEREBY EXPRESSLY DISCLAIMS ALL WARRANTIES, EITHER EXPRESS OR 
 ** IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 ** MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. Some jurisdictions do 
 ** not allow for the exclusion or limitation of implied warranties, so the above 
 ** limitations or exclusions may not apply to you. IBM shall not be liable for 
 ** any damages you suffer as a result of using, copying, modifying or 
 ** distributing the Sample, even if IBM has been advised of the possibility of 
 ** such damages.
 *****************************************************************************/

package com.ibm.replication.iidr.warehouse;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.ibm.replication.cdc.scripting.EmbeddedScript;
import com.ibm.replication.cdc.scripting.EmbeddedScriptException;
import com.ibm.replication.cdc.scripting.Result;
import com.ibm.replication.cdc.scripting.ResultStringTable;
import com.ibm.replication.iidr.utils.Bookmarks;
import com.ibm.replication.iidr.utils.MetricsDefinitions;
import com.ibm.replication.iidr.utils.Settings;
import com.ibm.replication.iidr.utils.Timer;
import com.ibm.replication.iidr.utils.Utils;
import com.ibm.replication.iidr.warehouse.logging.LogCsv;
import com.ibm.replication.iidr.warehouse.logging.LogDatabase;

public class CollectCDCStats {

	private CollectCDCStatsParms parms;

	private EmbeddedScript script;
	private Result result;
	private ResultStringTable subscriptionList;

	private Settings settings;
	private Bookmarks bookmarks;
	private MetricsDefinitions metricsDefinitions;

	private boolean connectAccessServer = true;
	private boolean connectDatabase = true;
	private boolean initCsv = true;

	static Logger logger;

	Timer timer;

	LogDatabase logDatabase;
	LogCsv logCsv;

	Locale currentLocale;
	NumberFormat localNumberFormat;

	private volatile boolean keepOn = true;

	HashMap<String, ArrayList<String>> subscriptionMetrics = new HashMap<String, ArrayList<String>>();
	HashMap<String, LinkedHashMap<String, Integer>> subscriptionMetricsMap = new HashMap<String, LinkedHashMap<String, Integer>>();

	public CollectCDCStats(CollectCDCStatsParms parms)
			throws ConfigurationException, EmbeddedScriptException, IllegalAccessException, InstantiationException,
			ClassNotFoundException, SQLException, IOException, CollectCDCStatsParmsException {

		this.parms = parms;

		PropertiesConfiguration versionInfo = new PropertiesConfiguration(
				"conf" + File.separator + "version.properties");

		logger = LogManager.getLogger();

		logger.info("Version: " + versionInfo.getString("buildVersion") + "." + versionInfo.getString("buildRelease")
				+ "." + versionInfo.getString("buildMod") + ", date: " + versionInfo.getString("buildDate"));

		// Debug logging?
		if (parms.debug) {
			LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			Configuration config = ctx.getConfiguration();
			LoggerConfig loggerConfig = config.getLoggerConfig("com.ibm.replication.iidr");
			loggerConfig.setLevel(Level.DEBUG);
			ctx.updateLoggers();
		}

		// Load settings
		settings = new Settings(parms.propertiesFile);

		// TODO Set locale hard-coded to avoid date conversion errors, to be
		// fixed later
		Locale.setDefault(new Locale("en", "US"));

		// Get current session's locale
		currentLocale = Locale.getDefault();
		logger.debug("Current locale (language_country): " + currentLocale.toString()
				+ ", metrics will be parsed according to this locale");
		localNumberFormat = NumberFormat.getInstance(currentLocale);

		// Check if the event log bookmarks will be used
		if (settings.logEventsToDB || settings.logEventsToCsv) {
			bookmarks = new Bookmarks("EventLogBookmarks.properties");
		}

		// Start the timer thread (controls execution of time-based activities)
		timer = new Timer(settings);
		new Thread(timer).start();

		// Create a script object to be used to execute CHCCLP commands
		script = new EmbeddedScript();
		try {
			script.open();
			while (keepOn) {
				// If the reset timer has been reached, disconnect from the
				// database and Access Server and rebuild the list of
				// subscriptions. Also reinitialize CSV writing
				if (timer.isTimerActivityDueMins(settings.getConnectionResetFrequency())) {
					connectAccessServer = true;
					connectDatabase = true;
					initCsv = true;
				}
				// Process the subscriptions one by one to collect the
				// information
				processSubscriptions();
				// Always sleep for 1 second, the frequency of the operations is
				// depicted in the properties file
				Thread.sleep(1000);
			}
		} catch (EmbeddedScriptException e1) {
			logger.error(e1.getMessage());
			throw new EmbeddedScriptException(99, "Error while running CHCCLP script");
		} catch (InterruptedException e) {
			logger.info("Stop of program requested");
		} catch (Exception e2) {
			logger.error("Error while collecting status and statistics: " + e2.getMessage());
		} finally {
			disconnectServerDS();
			script.close();
			disconnectFromDatabase();
		}
	}

	private void connectServerDS() {
		// First disconnect before trying to connect
		disconnectServerDS();
		// Connect to the Access Server, datastore and retrieve list of
		// subscriptions
		try {
			logger.info("Connecting to the access server " + settings.asHostName);
			script.execute("connect server hostname " + settings.asHostName + " port " + settings.asPort + " username "
					+ settings.asUserName + " password \"" + settings.asPassword + "\"");
			logger.info("Connecting to source datastore " + parms.datastore);
			script.execute("connect datastore name " + parms.datastore + " context source");
			// Reinitialize the list of subscriptions
			logger.debug("Get list of subscriptions");
			script.execute("list subscriptions filter datastore");
			result = script.getResult();
			subscriptionList = (ResultStringTable) result;
			connectAccessServer = false;
		} catch (EmbeddedScriptException e) {
			logger.error("Failed to connect to access server or datastore, or failed to get list of subscriptions: "
					+ script.getResultMessage());
			logger.error("Result Code : " + script.getResultCode());
		}
	}

	private void disconnectServerDS() {
		try {
			logger.debug("Disconnecting from source datastore " + parms.datastore);
			script.execute("disconnect datastore name " + parms.datastore);
		} catch (EmbeddedScriptException ignore) {
		}

		try {
			logger.debug("Disconnecting from access server " + settings.asHostName);
			script.execute("disconnect server");
		} catch (EmbeddedScriptException ignore) {
		}
	}

	private void connectToDatabase()
			throws IllegalAccessException, InstantiationException, ClassNotFoundException, IOException {
		// First try to disconnect from the database, in case this is not the
		// first time
		disconnectFromDatabase();
		// Only if events must be logged to database, open the connection
		if (settings.logEventsToDB || settings.logMetricsToDB || settings.logSubscriptionStatusToDB) {
			logDatabase = new LogDatabase(settings);
			connectDatabase = false;
		}
	}

	private void disconnectFromDatabase() {
		if (logDatabase != null)
			logDatabase.finish();
	}

	private void initializeCsv() {
		// Log to CSV if specified
		if (settings.logEventsToCsv || settings.logMetricsToCsv || settings.logSubscriptionStatusToCsv)
			logCsv = new LogCsv(settings);
	}

	private void processSubscriptions() throws IllegalAccessException, InstantiationException, ClassNotFoundException,
			SQLException, IOException, ConfigurationException {

		// If this is the first time, or when triggered by an interval or
		// connection error, connect to Access Server and the source datastore
		if (connectAccessServer)
			connectServerDS();

		// If this is the first time, or when triggered by an interval or
		// database error, re-establish the connection to the database
		if (connectDatabase)
			connectToDatabase();

		// If this is the first time, or when triggered by an interval,
		// reinitialize CSV processing
		if (initCsv)
			initializeCsv();

		// Get current timestamp
		Calendar cal = Calendar.getInstance();
		Timestamp collectTimestamp = new Timestamp(cal.getTimeInMillis());

		// Process all subscriptions listed before and retrieve info if
		// triggered
		for (int i = 0; i < subscriptionList.getRowCount(); i++) {
			String subscriptionName = subscriptionList.getValueAt(i, "SUBSCRIPTION");
			String targetDatastore = subscriptionList.getValueAt(i, "TARGET DATASTORE");
			// Collect status and metrics for subscription (if selected and
			// triggered by timer)
			if (timer.isTimerActivityDueSecs(settings.getSubscriptionCheckFrequency(subscriptionName))) {
				if (parms.subscriptionList == null || parms.subscriptionList.contains(subscriptionName))
					collectSubscriptionInfo(collectTimestamp, subscriptionName, parms.datastore, targetDatastore);
				else
					logger.debug(
							"Subscription " + subscriptionName + " skipped, not in list of selected subscriptions");
			}
		}

	}

	private void collectSubscriptionInfo(Timestamp collectTimestamp, String subscriptionName, String datastore,
			String targetDatastore) throws ConfigurationException, FileNotFoundException, IOException {
		logger.info("Collecting status and statistics for subscription " + subscriptionName + ", replicating from "
				+ datastore + " to " + targetDatastore);

		boolean targetConnected = true;
		// If target datastore different from source, connect to it
		if (!datastore.equals(targetDatastore)) {
			try {
				logger.debug("Connecting to target datastore " + targetDatastore);
				script.execute("connect datastore name " + targetDatastore + " context target");
			} catch (EmbeddedScriptException e) {
				logger.error("Could not connect to target datastore " + targetDatastore + ", error: "
						+ e.getResultCodeAndMessage());
				logger.error("Statistics of subscription " + subscriptionName + " will not be collected");
				targetConnected = false;
			}
		}
		if (targetConnected) {
			logSubscription(subscriptionName, datastore, targetDatastore, collectTimestamp);
		}

		// If there was a target datastore that was connected to,
		// disconnect
		if (!parms.datastore.equals(targetDatastore)) {
			try {
				script.execute("disconnect datastore name " + targetDatastore);
			} catch (EmbeddedScriptException e) {
				logger.debug(
						"Error disconnecting from target datastore " + targetDatastore + ", you can ignore this error");
			}
		}
	}

	/**
	 * Log subscription status, metrics and event log
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ConfigurationException
	 */
	private void logSubscription(String subscriptionName, String datastore, String targetDatastore,
			Timestamp collectTimestamp) throws ConfigurationException, FileNotFoundException, IOException {
		try {
			script.execute("select subscription name " + subscriptionName);
			script.execute("monitor replication filter subscription");
			ResultStringTable subscriptionMonitor = (ResultStringTable) script.getResult();

			String subscriptionState = subscriptionMonitor.getValueAt(0, "STATE");

			if (settings.logSubscriptionStatusToDB || settings.logSubscriptionStatusToCsv)
				logSubscriptionStatus(subscriptionName, collectTimestamp, subscriptionMonitor);

			if (settings.logMetricsToDB || settings.logMetricsToCsv) {
				if (subscriptionState.startsWith("Mirror"))
					logSubscriptionMetrics(subscriptionName, collectTimestamp);
				else
					logger.info("Current state for subscription " + subscriptionName + ": " + subscriptionState
							+ ", metrics not logged");

				// Harden the metrics that have been logged
				if (logDatabase != null)
					logDatabase.harden();
				if (logCsv != null)
					logCsv.harden();
			}

			if (settings.logEventsToDB || settings.logEventsToCsv)
				logEvents(subscriptionName, datastore, targetDatastore);

		} catch (EmbeddedScriptException e) {
			logger.error("Error collecting status or statistics from subscription " + subscriptionName + ". Error: "
					+ e.getResultCodeAndMessage());
		} catch (SQLException sqle) {
			logger.error("SQL Exception: " + sqle.getMessage() + ". Will attempt to reconnect to the database");
			connectDatabase = true;
		}
	}

	/**
	 * Log the subscription status
	 */
	private void logSubscriptionStatus(String subscriptionName, Timestamp collectTimestamp,
			ResultStringTable subscriptionStatus) throws SQLException {
		logger.debug("Obtaining the state of subscription " + subscriptionName);
		String subscriptionState = subscriptionStatus.getValueAt(0, "STATE");
		logger.debug("State of subscription " + subscriptionName + " is " + subscriptionState);
		// Now insert the subscription state into the table
		if (settings.logSubscriptionStatusToDB)
			logDatabase.logSubscriptionStatus(parms.datastore, subscriptionName, collectTimestamp, subscriptionState);
		if (settings.logSubscriptionStatusToCsv)
			logCsv.logSubscriptionStatus(parms.datastore, subscriptionName, collectTimestamp, subscriptionState);
	}

	/**
	 * Log the subscription metrics
	 * 
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ConfigurationException
	 */
	private void logSubscriptionMetrics(String subscriptionName, Timestamp collectTimestamp)
			throws EmbeddedScriptException, SQLException, ConfigurationException, FileNotFoundException, IOException {

		ArrayList<String> metricIDList = new ArrayList<String>();
		LinkedHashMap<String, Integer> metricDescriptionMap = new LinkedHashMap<String, Integer>();
		if (metricsDefinitions == null)
			metricsDefinitions = new MetricsDefinitions();

		// If the metrics for the subscriptions have not been retrieved yet, get
		// metric IDs
		if (!subscriptionMetrics.containsKey(subscriptionName)) {
			logger.debug("Obtaining available metrics of subscription " + subscriptionName);
			// Which performance metrics are available
			script.execute("list subscription performance metrics name " + subscriptionName);
			ResultStringTable availableMetrics = (ResultStringTable) script.getResult();
			String currentGroup = "";
			for (int r = 0; r < availableMetrics.getRowCount(); r++) {
				if (availableMetrics.getValueAt(r, 1).isEmpty()) {
					currentGroup = availableMetrics.getValueAt(r, 0);
				} else {
					String metricID = availableMetrics.getValueAt(r, 1);
					String metricDescription = availableMetrics.getValueAt(r, 0).trim();
					metricsDefinitions.checkAndWriteMetricDefinition(metricID, metricDescription);
					// Only include metric if in include list and not in exclude
					// list
					if ((settings.includeMetricsList.isEmpty() || settings.includeMetricsList.contains(metricID))
							&& !settings.excludeMetricsList.contains(metricID)) {
						metricIDList.add(availableMetrics.getValueAt(r, 1));
						metricDescriptionMap.put(currentGroup + " - " + metricDescription, Integer.parseInt(metricID));
					}
				}
			}
			logger.debug("Available metrics reported by engine: " + metricIDList);
			// Now check if the metric IDs are actually available
			boolean metricsChecked = false;
			while (!metricsChecked) {
				try {
					logger.debug("Checking adjusted list of metrics: " + metricIDList);
					String metricIDs = StringUtils.join(metricIDList, ",");
					script.execute("monitor subscription performance name " + subscriptionName + " metricIDs \""
							+ metricIDs + "\"");
					// If the program gets here, the metrics were valid
					metricsChecked = true;
				} catch (EmbeddedScriptException e) {
					if (script.getResultCode() == -2004) {
						logger.debug("Invalid metrics found in the list, error message: "
								+ script.getResultCodeAndMessage());
						metricIDList = removeInvalidMetrics(script.getResultMessage(), metricIDList);
						metricsChecked = false;
					} else
						throw new EmbeddedScriptException(script.getResultCode(), script.getResultMessage());
				}
			}

			logger.debug("Metric IDs for subscription " + subscriptionName + ": " + metricIDList);
			logger.debug("Metrics map for subscription " + subscriptionName + ": " + metricDescriptionMap);
			subscriptionMetrics.put(subscriptionName, metricIDList);
			subscriptionMetricsMap.put(subscriptionName, metricDescriptionMap);
		}

		String metricIDs = StringUtils.join(subscriptionMetrics.get(subscriptionName), ",");
		metricDescriptionMap = subscriptionMetricsMap.get(subscriptionName);

		// Now get the metrics
		script.execute(
				"monitor subscription performance name " + subscriptionName + " metricIDs \"" + metricIDs + "\"");
		ResultStringTable metrics = (ResultStringTable) script.getResult();
		logger.debug("Number of metrics retrieved: " + metrics.getRowCount());

		String metricSourceTarget = "";
		for (int r = 0; r < metrics.getRowCount(); r++) {
			String metric = metrics.getValueAt(r, 0).trim();
			if (metric.equals("Source") || metric.equals("Target")) {
				metricSourceTarget = metric.substring(0, 1);
			} else {
				Integer metricID = metricDescriptionMap.get(metric);
				String metricValue = metrics.getValueAt(r, 1);
				logger.debug(r + " : " + metricSourceTarget + " - " + metricID + " - " + metric + " - " + metricValue);
				Number metricNumber;
				try {
					metricNumber = localNumberFormat.parse(metricValue);
				} catch (ParseException e) {
					logger.error("Error while parsing value for metric ID " + metricID + ", value is " + metricValue
							+ ". Error: " + e.getMessage());
					metricNumber = 0;
				}
				if (settings.logMetricsToDB)
					logDatabase.logMetrics(parms.datastore, subscriptionName, collectTimestamp, metricSourceTarget,
							metricID, (long) metricNumber);
				if (settings.logMetricsToCsv)
					logCsv.logMetrics(parms.datastore, subscriptionName, collectTimestamp, metricSourceTarget, metricID,
							(long) metricNumber);
			}
		}
	}

	/**
	 * Removes the invalid metric IDs from the list
	 * 
	 * @param resultMessage
	 *            Error message containing the invalid metrics
	 * @param metricIDList
	 *            Metric ID list to be adjusted
	 * @return Adjusted metric ID list
	 */
	private ArrayList<String> removeInvalidMetrics(String resultMessage, ArrayList<String> metricIDList) {
		ArrayList<String> returnMetricIDList = new ArrayList<String>(metricIDList);
		Matcher metricsMatcher = Pattern.compile("\\d+(,\\d+)*").matcher(resultMessage);
		if (metricsMatcher.find()) {
			String metrics[] = metricsMatcher.group(0).split(",");
			for (String metricID : metrics) {
				logger.debug("Removing metric ID " + metricID + " from the list");
				returnMetricIDList.remove(metricID);
			}
		}
		return returnMetricIDList;
	}

	/**
	 * Log the subscription events
	 * 
	 * @throws EmbeddedScriptException
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws ConfigurationException
	 */
	private void logEvents(String subscriptionName, String sourceDatastore, String targetDatastore)
			throws SQLException, EmbeddedScriptException {
		logger.debug("Obtaining the log events of subscription " + subscriptionName);
		// Log source datastore events
		script.execute("list datastore events type source count " + settings.numberOfEvents);
		ResultStringTable sourceDataStoreEvents = (ResultStringTable) script.getResult();
		processEvents(sourceDataStoreEvents, sourceDatastore, null, "S");

		script.execute("list datastore events type target");
		ResultStringTable targetDataStoreEvents = (ResultStringTable) script.getResult();
		processEvents(targetDataStoreEvents, targetDatastore, null, "T");

		script.execute("list subscription events type source");
		ResultStringTable sourceSubscriptionEvents = (ResultStringTable) script.getResult();
		processEvents(sourceSubscriptionEvents, sourceDatastore, subscriptionName, "S");

		script.execute("list subscription events type target");
		ResultStringTable targetSubscriptionEvents = (ResultStringTable) script.getResult();
		processEvents(targetSubscriptionEvents, sourceDatastore, subscriptionName, "T");

	}

	/**
	 * Process the events that were collected and write them to the designated
	 * destinations
	 * 
	 * @throws SQLException
	 * @throws ArrayIndexOutOfBoundsException
	 */
	private void processEvents(ResultStringTable eventTable, String datastore, String subscriptionName,
			String sourceTarget) throws SQLException {
		String lastLoggedTimestamp = bookmarks.getEventBookmark(datastore, subscriptionName, sourceTarget);
		// First find the events which a later timestamp than the bookmark
		int lastRow = 0;
		for (int r = 1; r < eventTable.getRowCount(); r++) {
			String origEventTimestamp = eventTable.getValueAt(r, "TIME");
			String eventTimestamp = Utils.convertLogDateToIso(origEventTimestamp, settings.eventLogTimestampFormat);
			if (eventTimestamp.compareTo(lastLoggedTimestamp) > 0)
				lastRow = r;
		}
		// Now that the earliest event to be logged has been found, start
		// logging
		for (int r = lastRow; r > 0; r--) {
			String eventTimestamp = Utils.convertLogDateToIso(eventTable.getValueAt(r, "TIME"),
					settings.eventLogTimestampFormat);
			logger.debug("Event logged: " + datastore + "|" + subscriptionName + "|" + sourceTarget + "|"
					+ eventTable.getValueAt(r, "EVENT ID") + "|" + eventTable.getValueAt(r, "TYPE") + "|"
					+ eventTimestamp + "|" + eventTable.getValueAt(r, "MESSAGE"));
			if (settings.logEventsToDB)
				logDatabase.logEvent(datastore, subscriptionName, sourceTarget, eventTable.getValueAt(r, "EVENT ID"),
						eventTable.getValueAt(r, "TYPE"), eventTimestamp, eventTable.getValueAt(r, "MESSAGE"));
			if (settings.logEventsToCsv)
				logCsv.logEvent(datastore, subscriptionName, sourceTarget, eventTable.getValueAt(r, "EVENT ID"),
						eventTable.getValueAt(r, "TYPE"), eventTimestamp, eventTable.getValueAt(r, "MESSAGE"));
			lastLoggedTimestamp = eventTimestamp;
		}
		// The lastLoggedTimestamp has been kept up to date, now update bookmark
		bookmarks.writeEventBookmark(datastore, subscriptionName, sourceTarget, lastLoggedTimestamp);

	}

	public static void main(String[] args) {

		// Only set arguments when testing
		if (args.length == 1 && args[0].equalsIgnoreCase("*Testing*")) {
			args = "-d -ds DB2".split(" ");
			// args = "-d -ds DB2 -s CDC_BD,CDC_BS".split(" ");
			// args = "-d".split(" ");
		}

		// First check parameters
		CollectCDCStatsParms parms = null;
		try {
			// Get and check parameters
			parms = new CollectCDCStatsParms(args);
		} catch (CollectCDCStatsParmsException cpe) {
			Logger logger = LogManager.getLogger();
			logger.error("Error while validating parameters: " + cpe.getMessage());
		}

		// Set Log4j properties and get logger
		System.setProperty("log4j.configurationFile", System.getProperty("user.dir") + File.separatorChar + "conf"
				+ File.separatorChar + parms.loggingConfigurationFile);
		Logger logger = LogManager.getLogger();

		// Collect the statistics
		try {
			new CollectCDCStats(parms);
		} catch (Exception e) {
			// Report the full stack trace for debugging
			logger.error(Arrays.toString(e.getStackTrace()));
			logger.error("Error while collecting statistics from CDC: " + e.getMessage());
		}
	}
}