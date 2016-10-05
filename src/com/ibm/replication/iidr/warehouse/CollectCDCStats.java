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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;

import java.io.File;

import org.apache.commons.configuration.ConfigurationException;
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
import com.ibm.replication.iidr.utils.Settings;
import com.ibm.replication.iidr.warehouse.logging.LogCsv;
import com.ibm.replication.iidr.warehouse.logging.LogDatabase;

public class CollectCDCStats {

	private CollectCDCStatsParms parms;

	EmbeddedScript script;
	Result result;
	String sqlStatement;

	Settings settings;

	boolean connectAccessServer = true;
	boolean connectDatabase = true;

	static Logger logger;

	LogDatabase logDatabase;
	LogCsv logCsv;

	private volatile boolean keepOn = true;

	public CollectCDCStats(String[] commandLineArguments)
			throws ConfigurationException, EmbeddedScriptException, IllegalAccessException, InstantiationException,
			ClassNotFoundException, SQLException, IOException, CollectCDCStatsParmsException {

		parms = new CollectCDCStatsParms(commandLineArguments);

		// Set Log4j properties
		System.setProperty("log4j.configurationFile",
				new File(".", File.separatorChar + "conf" + File.separatorChar + "log4j2.xml").toURI().toURL()
						.toString());
		logger = LogManager.getLogger(CollectCDCStats.class.getName());
		// Debug logging?
		if (parms.debug) {
			LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
			Configuration config = ctx.getConfiguration();
			LoggerConfig loggerConfig = config.getLoggerConfig("com.ibm.replication.iidr");
			loggerConfig.setLevel(Level.DEBUG);
			ctx.updateLoggers();
			// LogManager.getRootLogger().setLevel(Level.DEBUG);
		}

		settings = new Settings("conf" + File.separator + "CollectCDCStats.properties");

		// Create a script object to be used to execute CHCCLP commands
		script = new EmbeddedScript();
		try {
			script.open();
			while (keepOn) {
				processSubscriptions();
				logger.info("Sleeping for " + settings.checkFrequencySeconds + " seconds");
				Thread.sleep(settings.checkFrequencySeconds * 1000);
			}
		} catch (EmbeddedScriptException e1) {
			logger.error(e1.getMessage());
			throw new EmbeddedScriptException(99, "Error in running CHCCLP script");
		} catch (InterruptedException e) {
			logger.info("Stop of program requested");
		} catch (Exception e2) {
			logger.error("Error while collecting status and statistics: " + e2.getMessage());
		} finally {
			script.close();
			if (settings.logToDatabase)
				logDatabase.finish();
		}
	}

	private void connectServerDS() {
		try {
			logger.info("Connecting to the access server " + settings.asHostName);
			script.execute("connect server hostname " + settings.asHostName + " port " + settings.asPort + " username "
					+ settings.asUserName + " password " + settings.asPassword);
			logger.info("Connecting to source datastore " + parms.datastore);
			script.execute("connect datastore name " + parms.datastore + " context source");
		} catch (EmbeddedScriptException e) {
			logger.error("Failed to connect to access server or datastore: " + script.getResultMessage());
			logger.error("Result Code : " + script.getResultCode());
		}
	}

	private void processSubscriptions()
			throws IllegalAccessException, InstantiationException, ClassNotFoundException, SQLException, IOException {

		// If this is the first time, or when a connection error has occurred,
		// connect to Access Server and the source datastore
		if (connectAccessServer) {
			connectServerDS();
			connectAccessServer = false;
		}

		// If this is the first time, or when a database error has occurred,
		// re-establish the connection to the database
		if (connectDatabase && settings.logToDatabase) {
			logDatabase = new LogDatabase(settings);
			connectDatabase = false;
		}

		// Log to CSV if specified
		if (settings.logToCSV)
			logCsv = new LogCsv(settings);

		// Get current timestamp
		Calendar cal = Calendar.getInstance();
		Timestamp collectTimestamp = new Timestamp(cal.getTimeInMillis());

		try {
			// Subscription routine
			logger.debug("Get list of subscriptions");
			script.execute("list subscriptions filter datastore");
			result = script.getResult();
			ResultStringTable subscriptionList = (ResultStringTable) result;
			for (int i = 0; i < subscriptionList.getRowCount(); i++) {
				String subscriptionName = subscriptionList.getValueAt(i, "SUBSCRIPTION");
				String targetDatastore = subscriptionList.getValueAt(i, "TARGET DATASTORE");
				// Collect status and metrics for subscription
				collectSubscriptionInfo(collectTimestamp, subscriptionName, targetDatastore);
			}
		} catch (EmbeddedScriptException e) {
			logger.error("Failed to monitor replication, will reconnect to Access Server and Datastore. Error message: "
					+ e.getResultCodeAndMessage());
			connectAccessServer = true;
		}
	}

	private void collectSubscriptionInfo(Timestamp collectTimestamp, String subscriptionName, String targetDatastore) {
		logger.info("Collecting status and statistics for subscription " + subscriptionName + ", replicating from "
				+ parms.datastore + " to " + targetDatastore);
		boolean targetConnected = true;
		// If target datastore different from source, connect to it
		if (!parms.datastore.equals(targetDatastore)) {
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
			logSubscription(subscriptionName, collectTimestamp);
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
	 */
	private void logSubscription(String subscriptionName, Timestamp collectTimestamp) {
		try {
			script.execute("select subscription name " + subscriptionName);
			script.execute("monitor replication filter subscription");
			ResultStringTable subscriptionStatus = (ResultStringTable) script.getResult();

			logSubscriptionStatus(subscriptionName, collectTimestamp, subscriptionStatus);

			if (subscriptionStatus.getValueAt(0, "STATE").startsWith("Mirror"))
				logSubscriptionMetrics(subscriptionName, collectTimestamp, subscriptionStatus);
			else
				logger.info("Current state for subscription " + subscriptionName + ": "
						+ subscriptionStatus.getValueAt(0, "STATE"));

			// Harden the metrics that have been logged
			if (settings.logToDatabase)
				logDatabase.harden();
			if (settings.logToCSV)
				logCsv.harden();
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
		String subscriptionState = subscriptionStatus.getValueAt(0, "STATE");
		logger.debug("State of subscription " + subscriptionName + " is " + subscriptionState);
		// Now insert the subscription state into the table
		if (settings.logToDatabase)
			logDatabase.logSubscriptionStatus(parms.datastore, subscriptionName, collectTimestamp, subscriptionState);
		if (settings.logToCSV)
			logCsv.logSubscriptionStatus(parms.datastore, subscriptionName, collectTimestamp, subscriptionState);
	}

	/**
	 * Log the subscription metrics
	 */
	private void logSubscriptionMetrics(String subscriptionName, Timestamp collectTimestamp,
			ResultStringTable subscriptionStatus) throws EmbeddedScriptException, SQLException {
		// Which performance metrics are available
		script.execute("list subscription performance metrics name " + subscriptionName);
		ResultStringTable availableMetrics = (ResultStringTable) script.getResult();
		ArrayList<String> metricIDList = new ArrayList<String>();
		LinkedHashMap<String, Integer> metricDescriptionMap = new LinkedHashMap<String, Integer>();
		String currentGroup = "";
		for (int r = 0; r < availableMetrics.getRowCount(); r++) {
			if (availableMetrics.getValueAt(r, 1).isEmpty()) {
				currentGroup = availableMetrics.getValueAt(r, 0);
			} else {
				if (!settings.ignoreMetricsList.contains(availableMetrics.getValueAt(r, 1))) {
					metricIDList.add(availableMetrics.getValueAt(r, 1));
					metricDescriptionMap.put(currentGroup + " - " + availableMetrics.getValueAt(r, 0).trim(),
							Integer.parseInt(availableMetrics.getValueAt(r, 1)));
				}
			}
		}

		String metricIDs = StringUtils.join(metricIDList, ",");

		logger.debug("Metrics map: " + metricDescriptionMap);

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
				logger.debug(r + " : " + metricSourceTarget + " - " + metricID + " - " + metric + " - "
						+ metrics.getValueAt(r, 1));
				long metricValue = Long.parseLong(metrics.getValueAt(r, 1).replaceAll(",", ""));
				if (settings.logToDatabase)
					logDatabase.logMetrics(parms.datastore, subscriptionName, collectTimestamp, metricSourceTarget,
							metricID, metricValue);
				if (settings.logToCSV)
					logCsv.logMetrics(parms.datastore, subscriptionName, collectTimestamp, metricSourceTarget, metricID,
							metricValue);
			}
		}

	}

	public static void main(String[] args) {

		// Only set arguments when testing
		if (args.length == 1 && args[0].equalsIgnoreCase("*Testing*")) {
			args = "-d -ds CDC_DB2".split(" ");
		}
		try {
			new CollectCDCStats(args);
		} catch (Exception e) {
			logger.error("Error while collecting statistics from CDC: " + e.getMessage());
		}
	}
}