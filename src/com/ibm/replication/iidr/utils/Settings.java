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
import java.io.FileNotFoundException;
import java.io.IOException;
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
	public boolean logMetricsToDB = true;
	public boolean logSubscriptionStatusToDB = true;
	public boolean logEventsToDB = false;
	public boolean logMetricsToCsv = false;
	public boolean logSubscriptionStatusToCsv = false;
	public boolean logEventsToCsv = true;

	public int numberOfEvents = 500;

	// Access Server connection parameters
	public String asHostName = null;
	public String asUserName = null;
	public String asPassword = null;
	public int asPort = 0;

	// Database connection parameters
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
	public String csvSeparator = "|";

	// Which metrics to include/exclude
	String includeMetrics;
	public ArrayList<String> includeMetricsList;
	String excludeMetrics;
	public ArrayList<String> excludeMetricsList;

	/**
	 * Retrieve the settings from the given properties file.
	 * 
	 * @param propertiesFile
	 * @throws ConfigurationException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public Settings(String propertiesFile) throws ConfigurationException, FileNotFoundException, IOException {

		logger = LogManager.getLogger();
		loadProperties(propertiesFile);
	}

	/**
	 * Load the properties
	 */
	private void loadProperties(String propertiesFile) throws ConfigurationException {
		PropertiesConfiguration config = new PropertiesConfiguration(
				System.getProperty("user.dir") + File.separatorChar + "conf" + File.separator + propertiesFile);

		checkFrequencySeconds = config.getInt("checkFrequencySeconds");

		logMetricsToDB = config.getBoolean("logMetricsToDB", logMetricsToDB);
		logSubscriptionStatusToDB = config.getBoolean("logSubscriptionStatusToDB", logSubscriptionStatusToDB);
		logEventsToDB = config.getBoolean("logEventsToDB", logEventsToDB);

		logMetricsToCsv = config.getBoolean("logMetricsToCsv", logMetricsToCsv);
		logSubscriptionStatusToCsv = config.getBoolean("logSubscriptionStatusToCsv", logSubscriptionStatusToCsv);
		logEventsToCsv = config.getBoolean("logEventsToCsv", logEventsToCsv);

		// Number of events to retrieve
		numberOfEvents = config.getInt("numberOfEvents", numberOfEvents);

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

		// Metrics to include
		includeMetrics = config.getString("includeMetrics");
		if (includeMetrics.isEmpty())
			includeMetricsList = new ArrayList<String>();
		else
			includeMetricsList = new ArrayList<String>(Arrays.asList(includeMetrics.split(",")));

		// Metrics to exclude
		excludeMetrics = config.getString("excludeMetrics");
		excludeMetricsList = new ArrayList<String>(Arrays.asList(excludeMetrics.split(",")));

		// Database connection settings
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
		csvSeparator = config.getString("csvSeparator", csvSeparator);

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

	public static void main(String[] args) throws ConfigurationException, IllegalArgumentException,
			IllegalAccessException, FileNotFoundException, IOException {
		System.setProperty("log4j.configurationFile",
				System.getProperty("user.dir") + File.separatorChar + "conf" + File.separatorChar + "log4j2.xml");
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig("com.ibm.replication.iidr.utils.Settings");
		loggerConfig.setLevel(Level.DEBUG);
		ctx.updateLoggers();
		new Settings("CollectCDCStats.properties");
	}

}
