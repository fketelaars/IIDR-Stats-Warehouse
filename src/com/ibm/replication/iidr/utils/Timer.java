/**
 * This subclass is used to run a background timer thread that checks if the
 * connection to the access server and datastore must be reset. The timer is started during 
 * initialization and stopped when the statistics collection ends
 */

package com.ibm.replication.iidr.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Timer implements Runnable {

	static Logger logger;

	private boolean stop = false;
	private boolean stopped = false;

	private Long currentTimer;
	private static final int INTERVAL = 1000;

	public Timer(Settings settings) {
		logger = LogManager.getLogger();

		currentTimer = new Long(0);
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
	 * seconds)
	 * 
	 */
	public boolean isTimerActivityDueSecs(int intervalSeconds) {
		if (intervalSeconds <= 0)
			return false;
		else
			return (currentTimer % intervalSeconds) == 0;
	}

	/**
	 * Returns whether or not the timer-based activity is due (specified in
	 * minutes)
	 * 
	 */
	public boolean isTimerActivityDueMins(int intervalMinutes) {
		if (intervalMinutes <= 0)
			return false;
		else
			return (currentTimer % (intervalMinutes * 60)) == 0;
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
					currentTimer += 1;
				}
			} catch (InterruptedException excp) {
			}
		}
		stopped = true;
	}
}
