package com.ibm.replication.iidr.warehouse.logging;

import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.ibm.replication.iidr.utils.Settings;

public abstract class LogInterface {

	protected Settings settings;
	static Logger logger;

	public LogInterface(Settings settings) {
		logger = LogManager.getLogger(this.getClass().getName());
		this.settings = settings;
	}

	/**
	 * Logs the status of the subscription
	 */
	public abstract void logSubscriptionStatus(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String subscriptionState) throws Exception;

	/**
	 * Logs the subscription statistics
	 */
	public abstract void logMetrics(String dataStore, String subscriptionName, Timestamp collectTimestamp,
			String metricSourceTarget, int metricID, long metricValue) throws Exception;

	/**
	 * Log the events
	 */
	public abstract void logEvent(String dataStore, String subscriptionName, String sourceTarget, String eventID,
			String eventType, String eventTimestamp, String eventMessage) throws Exception;
	
	/**
	 * Connect to the logger target
	 */
	public abstract void connect() throws Exception;
	
	/**
	 * Disconnect from the logger target
	 */
	public abstract void disconnect() throws Exception;

	/**
	 * Harden the written log records
	 */
	public abstract void harden() throws Exception;

	/**
	 * Final processing
	 */
	public abstract void finish() throws Exception;

}
