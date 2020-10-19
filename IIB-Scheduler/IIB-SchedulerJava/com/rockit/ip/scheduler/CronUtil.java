package com.rockit.ip.scheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class CronUtil {

	private static SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");

	public static String nextExecution(String exp, String dateString) {
		sdf.setTimeZone(TimeZone.getTimeZone("CET"));
		Date date;
		try {
			date = sdf.parse(dateString);
			CronManagerUtil cmu = new CronManagerUtil(exp);
			return sdf.format(cmu.next(date));

		} catch (ParseException e) {
			throw new RuntimeException(e);
		}

	}

	public static String nextExecution(String exp) {
		return nextExecution(exp, sdf.format(new Date()));

	}

}
