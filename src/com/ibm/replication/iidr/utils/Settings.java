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

package com.ibm.replication.iidr.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.configuration.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.datamirror.common.util.EncryptedDataException;
import com.datamirror.common.util.Encryptor;

public class Settings {

	static Logger logger;

	// Logging parameters
	public int checkFrequencySeconds = 60;
	public boolean logMetrics = true;
	public boolean logSubscriptionStatus = true;
	public boolean logEvents = true;

	// Access Server connection parameters
	public String asHostName = null;
	public String asUserName = null;
	public String asPassword = null;
	public int asPort = 0;

	// Database connection parameters
	public boolean logToDatabase = true;
	public String dbHostName;
	public int dbPort;
	public String dbDatabase;
	public String dbUserName;
	public String dbPassword;
	public String dbDriverName;
	public String dbUrl;
	public String dbSchema;
	public String sqlUrl;

	// CSV logging parameters
	public boolean logToCSV = false;
	public String statsDirectory;

	String ignoreMetrics;
	public ArrayList<String> ignoreMetricsList;

	/**
	 * Retrieve the settings from the given properties file.
	 * 
	 * @param propertiesFile
	 * @throws ConfigurationException
	 * @throws MalformedURLException
	 */
	public Settings(String propertiesFile) throws ConfigurationException, MalformedURLException {

		System.setProperty("log4j.configuration",
				new File(".", File.separatorChar + "conf" + File.separatorChar + "log4j.properties").toURI().toURL()
						.toString());
		logger = LogManager.getLogger(Settings.class.getName());

		PropertiesConfiguration config = new PropertiesConfiguration(propertiesFile);

		checkFrequencySeconds = config.getInt("checkFrequencySeconds");

		logMetrics = config.getBoolean("logMetrics", logMetrics);
		logSubscriptionStatus = config.getBoolean("logSubscriptionStatus", logSubscriptionStatus);
		logEvents = config.getBoolean("logEvents", logEvents);

		// Access Server settings
		asHostName = config.getString("asHostName");
		asUserName = config.getString("asUserName");
		String encryptedAsPassword = config.getString("asPassword");
		asPort = config.getInt("asPort", 10101);

		// Check if the password has already been encrypted
		// If not, encrypt and save the properties
		try {
			asPassword = Encryptor.decodeAndDecrypt(encryptedAsPassword);
		} catch (EncryptedDataException e) {
			logger.debug("Encrypting asPassword");
			asPassword = encryptedAsPassword;
			encryptedAsPassword = Encryptor.encryptAndEncode(encryptedAsPassword);
			config.setProperty("asPassword", encryptedAsPassword);
			config.save();
		}

		// Metrics to ignore
		ignoreMetrics = config.getString("ignoreMetrics");
		ignoreMetricsList = new ArrayList<String>(Arrays.asList(ignoreMetrics.split(",")));

		// Database connection settings
		logToDatabase = config.getBoolean("logToDatabase", logToDatabase);
		dbHostName = config.getString("dbHostName");
		dbPort = config.getInt("dbPort");
		dbDatabase = config.getString("dbDatabase");
		dbUserName = config.getString("dbUserName");
		String encryptedDbPassword = config.getString("dbPassword");
		dbDriverName = config.getString("dbDriverName");
		dbUrl = config.getString("dbUrl");
		dbSchema = config.getString("dbSchema");

		try {
			dbPassword = Encryptor.decodeAndDecrypt(encryptedDbPassword);
		} catch (EncryptedDataException e) {
			logger.debug("Encrypting dbPassword");
			dbPassword = encryptedDbPassword;
			encryptedDbPassword = Encryptor.encryptAndEncode(encryptedDbPassword);
			config.setProperty("dbPassword", encryptedDbPassword);
			config.save();
		}

		// CSV logging settings
		logToCSV = config.getBoolean("logToCSV", logToCSV);
		statsDirectory = config.getString("statsDirectory");

		// Now report the settings
		logSettings(config);
	}

	/**
	 * Log the properties in the specified configuration file
	 * 
	 * @param config
	 */
	private void logSettings(PropertiesConfiguration config) {
		Iterator<String> configKeys = config.getKeys();
		while (configKeys.hasNext()) {
			String configKey = configKeys.next();
			logger.debug("Property: " + configKey + " = " + config.getProperty(configKey));
		}
	}

	public static void main(String[] args)
			throws ConfigurationException, IllegalArgumentException, IllegalAccessException, MalformedURLException {
		System.setProperty("log4j.configurationFile",
				new File(".", File.separatorChar + "conf" + File.separatorChar + "log4j2.xml").toURI().toURL()
						.toString());
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
		loggerConfig.setLevel(Level.DEBUG);
		ctx.updateLoggers();
		new Settings("conf/CollectCDCStats.properties");
	}

}
