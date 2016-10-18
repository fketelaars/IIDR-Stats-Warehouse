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

package com.ibm.replication.iidr.warehouse;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.cli.*;

public class CollectCDCStatsParms {

	private Options options;
	private HelpFormatter formatter;
	private CommandLineParser parser;
	private CommandLine commandLine;

	public boolean debug;
	public String datastore;
	private String subscriptions;
	public ArrayList<String> subscriptionList;
	public String propertiesFile;
	public String loggingConfigurationFile;

	public CollectCDCStatsParms(String[] commandLineArguments) throws CollectCDCStatsParmsException {
		// Initialize parameters
		debug = false;
		datastore = "";
		formatter = new HelpFormatter();
		parser = new DefaultParser();
		options = new Options();
		subscriptions = null;

		options.addOption("d", false, "Show debug messages");
		options.addOption("ds", true, "Source datastore");
		options.addOption("s", true, "Subscription(s) to select. If not specified, all subscriptions"
				+ " of the selected source datastore will be included");
		options.addOption("p", true,
				"Properties file (must exist in the conf directory). Default is CollectCDCStats.properties");
		options.addOption("l", true,
				"Log4j2 configuration file (must exist in the conf directory). Default is log4j2.xml");

		try {
			commandLine = parser.parse(options, commandLineArguments);
		} catch (ParseException e) {
			sendInvalidParameterException("");
		}

		this.debug = commandLine.hasOption("d");

		// Datastore parameter is mandatory
		if (commandLine.getOptionValue("ds") != null) {
			datastore = commandLine.getOptionValue("ds");
			if (commandLine.getOptionValue("s") != null) {
				subscriptions = commandLine.getOptionValue("s");
				subscriptionList = new ArrayList<String>(Arrays.asList(subscriptions.split(",")));
			}
			if (commandLine.getOptionValue("p") != null) {
				propertiesFile = commandLine.getOptionValue("p");
			} else
				propertiesFile = "CollectCDCStats.properties";

			if (commandLine.getOptionValue("l") != null) {
				loggingConfigurationFile = commandLine.getOptionValue("l");
			} else
				loggingConfigurationFile = "log4j2.xml";

		} else
			sendInvalidParameterException("Datastore (ds parameter) must be specified");

	}

	// Method to send exception
	private void sendInvalidParameterException(String message) throws CollectCDCStatsParmsException {
		formatter.printHelp("CollectCDCStats", message, this.options, "", true);
		throw new CollectCDCStatsParmsException("Error while validating parameters");
	}

}
