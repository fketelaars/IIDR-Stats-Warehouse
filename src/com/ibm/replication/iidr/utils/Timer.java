/**
 * This subclass is used to run a background timer thread that checks if the
 * connection to the access server and datastore must be reset. The timer is started during 
 * initialization and stopped when the statistics collection ends
 */

package com.ibm.replication.iidr.utils;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Timer implements Runnable {

	static Logger logger;

	private boolean stop = false;
	private boolean stopped = false;

	private Long currentTimer;
	private static final int INTERVAL = 1000;

	private HashMap<String, Long> activityTimer;

	public Timer(Settings settings) {
		logger = LogManager.getLogger();
		currentTimer = System.currentTimeMillis();
		activityTimer = new HashMap<String, Long>();
	}

	/**
	 * Stops the thread
	 */
	public void stop() {
		stop = true;
	}

	/**
	 * Returns if the thread has been stopped
	 */
	public boolean isStopped() {
		return stopped;
	}

	/**
	 * Returns whether or not the timer-based activity is due (specified in
	 * seconds) for the subscription
	 * 
	 */
	public boolean isSubscriptionActivityDue(String datastore, String subscriptionName, int intervalSeconds) {
		boolean activityDue = false;
		if (intervalSeconds > 0) {
			String timerKey = datastore + "-" + subscriptionName;
			Long lastActivation = activityTimer.get(timerKey);
			if (lastActivation != null) {
				if (currentTimer >= (lastActivation + intervalSeconds * 1000)) {
					activityTimer.put(timerKey, currentTimer);
					activityDue = true;
				}
			} else {
				activityTimer.put(timerKey, currentTimer);
				activityDue = true;
			}
		}
		return activityDue;
	}

	/**
	 * Returns whether or not the timer-based activity is due (specified in
	 * minutes)
	 * 
	 */
	public boolean isTimerActivityDueMins(String timerKey, int intervalMinutes) {
		boolean activityDue = false;
		if (intervalMinutes > 0) {
			Long lastActivation = activityTimer.get(timerKey);
			if (lastActivation != null) {
				if (currentTimer >= (lastActivation + intervalMinutes * 60 * 1000)) {
					activityTimer.put(timerKey, currentTimer);
					activityDue = true;
				}
			} else {
				activityTimer.put(timerKey, currentTimer);
				activityDue = true;
			}
		}
		return activityDue;
	}

	/**
	 * Returns whether or not the timer interval has been reached
	 */
	public void resetTimer() {
		synchronized (currentTimer) {
			currentTimer = 0L;
		}
	}

	/**
	 * This method is run when the thread is started. It executed an infinite
	 * loop until the stop variable is set to true
	 */
	public void run() {
		logger.info("Timer started, controls interval-based activities");
		while (!stop) {
			try {
				Thread.sleep(INTERVAL);
				synchronized (currentTimer) {
					currentTimer = System.currentTimeMillis();
				}
			} catch (InterruptedException excp) {
			}
		}
		stopped = true;
	}
}
