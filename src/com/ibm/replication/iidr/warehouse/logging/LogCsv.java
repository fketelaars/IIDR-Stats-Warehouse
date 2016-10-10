package com.ibm.replication.iidr.warehouse.logging;

import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.ibm.replication.iidr.utils.Settings;

public class LogCsv extends LogInterface {

	Logger csvLogger;
	String statusHeader = null;
	String metricsHeader = null;
	String eventsHeader=null;

	public LogCsv(Settings settings) {
		super(settings);
		csvLogger = LogManager.getLogger(this.getClass());
		ThreadContext.put("separator", settings.csvSeparator);
		statusHeader = "dataStore" + settings.csvSeparator + "subscriptionName" + settings.csvSeparator
				+ "collectTimestamp" + settings.csvSeparator + "subscriptionState";
		metricsHeader = "dataStore" + settings.csvSeparator + "subscriptionName" + settings.csvSeparator
				+ "collectTimestamp" + settings.csvSeparator + "metricSourceTarget" + settings.csvSeparator + "metricID"
				+ settings.csvSeparator + "metricValue";
		eventsHeader = "dataStore" + settings.csvSeparator + "subscriptionName" + settings.csvSeparator
				+ settings.csvSeparator + "sourceTarget" + settings.csvSeparator + "eventID" + settings.csvSeparator
				+ "eventType" + settings.csvSeparator + "eventMessage";
	}

	/**
	 * Logs the status of the subscription into the flat file
	 * 
	 */
	@Override
	public void logSubscriptionStatus(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String subscriptionState) {
		ThreadContext.put("dataStore", dataStore);
		ThreadContext.put("subscriptionName", subscriptionName);
		ThreadContext.put("type", "SubStatus");
		ThreadContext.put("header", statusHeader);
		csvLogger.info("test", dataStore, subscriptionName, collectTimestamp, subscriptionState);
	}

	/**
	 * Logs the metrics into the flat file
	 */
	@Override
	public void logMetrics(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String metricSourceTarget, int metricID, long metricValue) {
		ThreadContext.put("dataStore", dataStore);
		ThreadContext.put("subscriptionName", subscriptionName);
		ThreadContext.put("type", "Statistics");
		ThreadContext.put("header", metricsHeader);
		csvLogger.info("ignore", dataStore, subscriptionName, collectTimestamp, metricSourceTarget, metricID,
				metricValue);
	}

	@Override
	public void logEvent(String dataStore, String subscriptionName, String sourceTarget, String eventID,
			String eventType, String eventTimestamp, String eventMessage) {
		ThreadContext.put("dataStore", dataStore);
		ThreadContext.put("subscriptionName", subscriptionName);
		ThreadContext.put("type", "Events");
		ThreadContext.put("header", eventsHeader);
		csvLogger.info("ignore", dataStore, subscriptionName, sourceTarget, eventID, eventType, eventTimestamp,
				eventMessage);
	}

	/**
	 * Hardens (commits) the written records
	 */
	@Override
	public void harden() {
	}

	/**
	 * Final processing (disconnect from the database)
	 */
	@Override
	public void finish() {
		logger.debug("Finalizing processing for logging to flatfile");
	}

}
