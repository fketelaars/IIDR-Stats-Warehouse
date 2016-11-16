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

	private int connectionResetFrequencyMin;
	private long connectionResetFrequency;
	private Long currentTimerMs;
	private static final int INTERVALMS = 1000;

	public Timer(Settings settings) {
		logger = LogManager.getLogger();

		currentTimerMs = new Long(0);
		connectionResetFrequencyMin = settings.connectionResetFrequencyMin;
		connectionResetFrequency = connectionResetFrequencyMin * 60 * 1000;
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
	 * Returns whether or not the handshake is due (timer interval has been
	 * reached)
	 */
	public boolean isConnectionResetDue() {
		return (currentTimerMs >= connectionResetFrequency);
	}

	/**
	 * Returns whether or not the handshake is due (timer interval has been
	 * reached)
	 */
	public void resetTimer() {
		synchronized (currentTimerMs) {
			currentTimerMs = 0L;
		}
	}

	/**
	 * This method is run when the thread is started. It executed an infinite
	 * loop until the stop variable is set to true
	 */
	public void run() {
		logger.info("Timer started, connection to the Access Server and source datastore " + "will be reset every "
				+ connectionResetFrequencyMin + " minutes");
		while (!stop) {
			try {
				Thread.sleep(INTERVALMS);
				synchronized (currentTimerMs) {
					currentTimerMs += INTERVALMS;
				}
			} catch (InterruptedException excp) {
			}
		}
		stopped = true;
	}
}
