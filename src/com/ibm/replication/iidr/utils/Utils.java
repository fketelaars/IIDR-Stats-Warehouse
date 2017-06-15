package com.ibm.replication.iidr.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Utils {

	private static SimpleDateFormat logDateFormat;
	private static SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	private static Logger logger = LogManager.getLogger();

	public static String convertLogDateToIso(String inputDate, String eventLogTimestampFormat) {

		if (logDateFormat == null)
			logDateFormat = new SimpleDateFormat(eventLogTimestampFormat);

		String isoDate = null;
		try {
			Date date = logDateFormat.parse(inputDate);
			isoDate = isoDateFormat.format(date);
		} catch (ParseException e) {
			logger.error("Error while parsing date " + inputDate + ": " + e.getMessage());
			isoDate = "0000-01-01 00:00:00";
		}
		return isoDate;
	}

}
