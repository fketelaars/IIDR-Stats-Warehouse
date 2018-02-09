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
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy;
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

	PropertiesConfiguration config = null;

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
		// Prepare the loading of the properties
		config = new PropertiesConfiguration(
				System.getProperty("user.dir") + File.separatorChar + "conf" + File.separator + propertiesFile);
		// Most properties can be changed dynamically without having to restart
		// the collection altogether. When a change in the properties file is,
		// the properties are automatically reloaded. To avoid hammering the
		// properties file, the reload frequency is maximized to every 10
		// seconds
		FileChangedReloadingStrategy reloadingStrategy = new FileChangedReloadingStrategy();
		reloadingStrategy.setRefreshDelay(10000);
		config.setReloadingStrategy(reloadingStrategy);

		// Now report the settings
		logSettings(config);
	}

	/*
	 * Get the property that contains the value. If a qualified property is
	 * specified, first the qualified property will be retrieved. If not found,
	 * the simple property will be retrieved.
	 */
	private String getResolvedName(String propertyName) {
		String resolvedName = null;
		if (config.containsKey(propertyName))
			resolvedName = propertyName;
		else {
			if (propertyName.contains(".")) {
				String simpleProperty = propertyName.substring(propertyName.lastIndexOf(".") + 1);
				if (config.containsKey(simpleProperty))
					resolvedName = simpleProperty;
			}
		}
		return resolvedName;
	}

	/*
	 * Get String property
	 */
	public String getString(String propertyName) {
		String propertyValue = null;
		String resolvedName = getResolvedName(propertyName);
		if (resolvedName != null)
			propertyValue = config.getString(resolvedName);
		return propertyValue;
	}

	/*
	 * Get String property
	 */
	public String getString(String propertyName, String defaultValue) {
		String propertyValue = defaultValue;
		String resolvedName = getResolvedName(propertyName);
		if (resolvedName != null)
			propertyValue = config.getString(resolvedName);
		return propertyValue;
	}

	/*
	 * Get Encrypted String property
	 */
	public String getEncryptedString(String propertyName) {
		String propertyValue = null;
		String resolvedName = getResolvedName(propertyName);
		if (resolvedName != null) {
			String value = config.getString(resolvedName);
			try {
				propertyValue = Encryptor.decodeAndDecrypt(value);
			} catch (EncryptedDataException e) {
				logger.debug("Encrypting property " + resolvedName);
				propertyValue = value;
				String encryptedValue = Encryptor.encryptAndEncode(value);
				config.setProperty(resolvedName, encryptedValue);
				try {
					config.save();
				} catch (ConfigurationException e1) {
					e1.printStackTrace();
				}
			}
		}
		return propertyValue;
	}

	// Check if the password has already been encrypted
	// If not, encrypt and save the properties

	/*
	 * Get Integer property
	 */
	public int getInt(String propertyName) {
		int propertyValue = 0;
		String resolvedName = getResolvedName(propertyName);
		if (resolvedName != null)
			propertyValue = config.getInt(resolvedName);
		return propertyValue;
	}

	/*
	 * Get Integer property
	 */
	public int getInt(String propertyName, int defaultValue) {
		int propertyValue = defaultValue;
		String resolvedName = getResolvedName(propertyName);
		if (resolvedName != null)
			propertyValue = config.getInt(resolvedName);
		return propertyValue;
	}

	/*
	 * Get String List property
	 */
	public ArrayList<String> getStringList(String propertyName) {
		ArrayList<String> propertyValue = null;
		String resolvedName = getResolvedName(propertyName);
		if (resolvedName != null)
			propertyValue = new ArrayList<String>(Arrays.asList(config.getStringArray(resolvedName)));
		return propertyValue;
	}

	/*
	 * Get String List property
	 */
	public ArrayList<String> getStringList(String propertyName, ArrayList<String> defaultValue) {
		ArrayList<String> propertyValue = defaultValue;
		String resolvedName = getResolvedName(propertyName);
		if (resolvedName != null)
			propertyValue = new ArrayList<String>(Arrays.asList(config.getStringArray(resolvedName)));
		return propertyValue;
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
			IllegalAccessException, FileNotFoundException, IOException, InterruptedException {
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
