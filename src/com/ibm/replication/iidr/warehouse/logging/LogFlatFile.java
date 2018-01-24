package com.ibm.replication.iidr.warehouse.logging;

import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.ibm.replication.iidr.utils.Settings;

public class LogFlatFile extends LogInterface {

	Logger ffLoggerSubscriptionStatus;
	Logger ffLoggerMetrics;
	Logger ffLoggerEvent;
	String statusHeader = null;
	String metricsHeader = null;
	String eventsHeader = null;

	public LogFlatFile(Settings settings) {
		super(settings);
		ffLoggerSubscriptionStatus = LogManager.getLogger(this.getClass().getName() + ".logSubscriptionStatus");
		ffLoggerMetrics = LogManager.getLogger(this.getClass().getName() + ".logMetrics");
		ffLoggerEvent = LogManager.getLogger(this.getClass().getName() + ".logEvent");
	}

	/**
	 * Logs the status of the subscription into the flat file
	 */
	@Override
	public void logSubscriptionStatus(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String subscriptionState) {
		ThreadContext.clearAll();
		ThreadContext.put("dataStore", dataStore);
		ThreadContext.put("subscriptionName", subscriptionName);
		ThreadContext.put("type", "SubStatus");
		ThreadContext.put("header", statusHeader);
		ThreadContext.put("collectTimestamp", collectTimestamp.toString());
		ThreadContext.put("subscriptionState", subscriptionState);
		ffLoggerSubscriptionStatus.info("ignore");
	}

	/**
	 * Logs the metrics into the flat file
	 */
	@Override
	public void logMetrics(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String metricSourceTarget, int metricID, long metricValue) {
		ThreadContext.clearAll();
		ThreadContext.put("dataStore", dataStore);
		ThreadContext.put("subscriptionName", subscriptionName);
		ThreadContext.put("type", "Statistics");
		ThreadContext.put("header", metricsHeader);
		ThreadContext.put("collectTimestamp", collectTimestamp.toString());
		ThreadContext.put("metricSourceTarget", metricSourceTarget);
		ThreadContext.put("metricID", Integer.toString(metricID));
		ThreadContext.put("metricValue", Long.toString(metricValue));
		ffLoggerMetrics.info("ignore");
	}

	@Override
	public void logEvent(String dataStore, String subscriptionName, String sourceTarget, String eventID,
			String eventType, String eventTimestamp, String eventMessage) {
		ThreadContext.clearAll();
		ThreadContext.put("dataStore", dataStore);
		ThreadContext.put("subscriptionName", subscriptionName);
		ThreadContext.put("type", "Events");
		ThreadContext.put("header", eventsHeader);
		ThreadContext.put("sourceTarget", sourceTarget);
		ThreadContext.put("eventID", eventID);
		ThreadContext.put("eventType", eventType);
		ThreadContext.put("eventTimestamp", eventTimestamp);
		ThreadContext.put("eventMessage", eventMessage);
		ffLoggerEvent.info("ignore");
	}

	/**
	 * Hardens (commits) the written records
	 */
	@Override
	public void harden() {
	}

	/**
	 * Final processing
	 */
	@Override
	public void finish() {
		logger.debug("Finalizing processing for logging to flatfile");
	}

	@Override
	public void connect() throws Exception {
	}

	@Override
	public void disconnect() throws Exception {
	}

}
