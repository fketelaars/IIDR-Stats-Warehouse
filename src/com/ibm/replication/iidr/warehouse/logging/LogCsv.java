package com.ibm.replication.iidr.warehouse.logging;

import java.sql.Timestamp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import com.ibm.replication.iidr.utils.Settings;

public class LogCsv extends LogInterface {

	Logger csvLogger;

	public LogCsv(Settings settings) {
		super(settings);
		csvLogger = LogManager.getLogger(this.getClass());
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
		csvLogger.info("ignore", dataStore, subscriptionName, collectTimestamp, metricSourceTarget, metricID,
				metricValue);
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
