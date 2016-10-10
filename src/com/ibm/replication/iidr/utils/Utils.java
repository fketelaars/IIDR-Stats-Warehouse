package com.ibm.replication.iidr.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {

	private static SimpleDateFormat logDateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a");
	private static SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public static String convertLogDateToIso(String inputDate) {

		String isoDate = null;
		try {
			Date date = logDateFormat.parse(inputDate);
			isoDate = isoDateFormat.format(date);
		} catch (ParseException e) {
		}
		return isoDate;
	}

}
