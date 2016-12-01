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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.commons.configuration.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

public class MetricsDefinitions {

	static Logger logger;

	// Metrics definitions
	PropertiesConfiguration metricsProperties;

	public MetricsDefinitions() throws ConfigurationException, FileNotFoundException, IOException {
		logger = LogManager.getLogger();
		loadMetrics();
	}

	/**
	 * Load the metrics definitions from the properties file
	 */
	private void loadMetrics() throws ConfigurationException, FileNotFoundException, IOException {
		String metricsDefinitionsFileName = System.getProperty("user.dir") + File.separatorChar + "conf"
				+ File.separator + "MetricsDefinitions.properties";
		File metricsDefinitionsFile = new File(metricsDefinitionsFileName);
		// Create the metrics definitions file if it doesn't exist (should not
		// be the case)
		if (!metricsDefinitionsFile.exists())
			new FileOutputStream(metricsDefinitionsFileName, true).close();
		// Get metrics definitions
		metricsProperties = new PropertiesConfiguration(metricsDefinitionsFile);
		Iterator<String> metricsDefinitionsKeys = metricsProperties.getKeys();
		while (metricsDefinitionsKeys.hasNext()) {
			String metricID = metricsDefinitionsKeys.next();
			logger.debug("Metric: " + metricID + " = " + (String) metricsProperties.getProperty(metricID));
		}
		// Make sure that any updates to the metrics definitions are
		// automatically saved
		metricsProperties.setAutoSave(true);
	}

	/**
	 * Check if the metric exists. If not, add it
	 */
	public void checkAndWriteMetricDefinition(String metricID, String metricDescription) {
		if (!metricsProperties.containsKey(metricID)) {
			logger.debug("Metric " + metricID + " not yet found in the properties"
					+ ", will be added with description: " + metricDescription.trim());
			metricsProperties.addProperty(metricID, metricDescription.trim());
		}
	}

	public static void main(String[] args) throws ConfigurationException, IllegalArgumentException,
			IllegalAccessException, FileNotFoundException, IOException {
		System.setProperty("log4j.configurationFile",
				System.getProperty("user.dir") + File.separatorChar + "conf" + File.separatorChar + "log4j2.xml");
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig("com.ibm.replication.iidr.utils.Metricsdefinitions");
		loggerConfig.setLevel(Level.DEBUG);
		ctx.updateLoggers();
		new MetricsDefinitions();
	}

}
