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

public class Bookmarks {

	static Logger logger;

	// Event log bookmarks
	PropertiesConfiguration bookmarks;

	/**
	 * Retrieve the settings from the given properties file.
	 * 
	 * @param bookmarksFile
	 * @throws ConfigurationException
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public Bookmarks(String bookmarksFileName) throws ConfigurationException, FileNotFoundException, IOException {
		logger = LogManager.getLogger();
		loadBookmarks(bookmarksFileName);
	}

	/**
	 * Load the bookmarks from the properties file
	 */
	private void loadBookmarks(String bookmarksFileName)
			throws ConfigurationException, FileNotFoundException, IOException {
		String bookmarkFullFileName = System.getProperty("user.dir") + File.separatorChar + "conf" + File.separator
				+ "bookmarks" + File.separator + bookmarksFileName;
		File bookmarkFile = new File(bookmarkFullFileName);
		// Create the bookmark file if it doesn't exist
		if (!bookmarkFile.exists()) {
			new FileOutputStream(bookmarkFullFileName, true).close();
		}
		// Get bookmarks
		bookmarks = new PropertiesConfiguration(bookmarkFile);
		Iterator<String> bookmarkKeys = bookmarks.getKeys();
		while (bookmarkKeys.hasNext()) {
			String bookmarkKey = bookmarkKeys.next();
			logger.debug("Bookmark: " + bookmarkKey + " = " + (String) bookmarks.getProperty(bookmarkKey));
		}
		// Make sure that any updates to the bookmarks is automatically saved
		bookmarks.setAutoSave(true);
	}

	/**
	 * Store event log bookmark
	 */
	public void writeEventBookmark(String dataStore, String subscriptionName, String sourceTarget,
			String bookmarkTimestamp) {
		bookmarks.setProperty(getBookmarkKey(dataStore, subscriptionName, sourceTarget), bookmarkTimestamp);
	}

	/**
	 * Get event log bookmark, if existent
	 */
	public String getEventBookmark(String dataStore, String subscriptionName, String sourceTarget) {
		String bookmarkTimestamp = "";
		bookmarkTimestamp = (String) bookmarks.getProperty(getBookmarkKey(dataStore, subscriptionName, sourceTarget));
		if (bookmarkTimestamp == null)
			bookmarkTimestamp = "1970-01-01 00:00:00";
		return bookmarkTimestamp;
	}

	/**
	 * Get bookmark key, based on datastore name and subscription name
	 */
	private String getBookmarkKey(String dataStore, String subscriptionName, String sourceTarget) {
		String bookmarkKey = "";
		if (subscriptionName != null && !subscriptionName.isEmpty())
			bookmarkKey = "Subscription-" + dataStore + "-" + subscriptionName + "-" + sourceTarget;
		else
			bookmarkKey = "Datastore-" + dataStore + "-" + sourceTarget;
		return bookmarkKey;
	}

	public static void main(String[] args) throws ConfigurationException, IllegalArgumentException,
			IllegalAccessException, FileNotFoundException, IOException {
		System.setProperty("log4j.configurationFile",
				System.getProperty("user.dir") + File.separatorChar + "conf" + File.separatorChar + "log4j2.xml");
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig("com.ibm.replication.iidr.utils.Bookmarks");
		loggerConfig.setLevel(Level.DEBUG);
		ctx.updateLoggers();
		new Bookmarks("EventLogBookmarks.properties");
	}

}
