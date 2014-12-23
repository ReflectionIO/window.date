//
//  Program.java
//  window.date
//
//  Created by William Shakour (billy1380) on 25 Aug 2014.
//  Copyright © 2014 SPACEHOPPER STUDIOS Ltd. All rights reserved.
//
package io.reflection.window.date;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import asg.cliche.Command;
import asg.cliche.InputConverter;
import asg.cliche.Param;
import asg.cliche.ShellFactory;

/**
 * @author William Shakour (billy1380)
 * 
 */
public class Program {

	public static final InputConverter[] CLI_INPUT_CONVERTERS = { new InputConverter() {
		public Object convertInput(String original, @SuppressWarnings("rawtypes") Class toClass) throws Exception {

			Object o = null;

			if (toClass.equals(Date.class)) {
				o = SDF.parse(original);
			}

			return o;
		}
	}, };

	private static final String LOGGER_CONFIG_PATH = "./Logger.xml";
	static {
		DOMConfigurator.configure(LOGGER_CONFIG_PATH);
	}

	private static final Program INSTANCE = new Program();

	public static final String DATE_FORMAT = "yyyy-MM-dd";
	private static final SimpleDateFormat SDF = new SimpleDateFormat(DATE_FORMAT);

	private static final Logger LOGGER = Logger.getLogger(Program.class);

	private static final Integer DEFAULT_DAYS_PER_SLICE = Integer.valueOf(30);
	private static final long MILLIS_PER_DAY = 24 * 60 * 60 * 1000;

	private Map<Date, Integer> dateRank = null;
	private Map<Long, List<Integer>> files = new HashMap<Long, List<Integer>>();

	private int DEFAULT_PERCENTAGE_DROPPED = 5;
	private int DEFAULT_LOWEST_RANK = 200;
	private int DEFAULT_DAYS = 20;

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		ShellFactory.createConsoleShell("window.date", "window.date", INSTANCE).commandLoop();
	}

	@Command(abbrev = "g0")
	public void generateRanks() {
		generateRanks(DEFAULT_PERCENTAGE_DROPPED, DEFAULT_LOWEST_RANK, DEFAULT_DAYS);
	}

	@Command
	public void generateRanks(@Param(name = "percentageDropped") int percentageDropped, @Param(name = "lowestRank") int lowestRank,
	        @Param(name = "days") int days) {

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Generating ranks with precentage dropped [" + percentageDropped + "], lowest rank [" + lowestRank + "] and days [" + days + "]");
		}

		if (dateRank == null) {
			dateRank = new HashMap<Date, Integer>();
		} else {
			dateRank.clear();
		}

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DAY_OF_YEAR, -days);

		Random rand = new Random();
		int rank;
		for (int i = 0; i < days; i++) {
			rank = rand.nextInt(lowestRank) + 1;

			c.add(Calendar.DAY_OF_YEAR, 1);

			if (rand.nextInt(100) > percentageDropped) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Added generated rank for [" + rank + "] and date [" + SDF.format(c.getTime()) + "]");
				}
				dateRank.put(c.getTime(), Integer.valueOf(rank));
			} else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Dropped generated rank for [" + rank + "] and date [" + SDF.format(c.getTime()) + "]");
				}
			}
		}
	}

	@Command(name = "p0")
	public void performSlicing() {
		performSlicing(null);
	}

	@Command
	public void performSlicing(@Param(name = "daysPerSlice") Integer daysPerSlice) {
		// if there is no rank data then generate it using the default values
		if (dateRank == null) {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Did not find generated ranks, generating with default values");
			}

			generateRanks(DEFAULT_PERCENTAGE_DROPPED, DEFAULT_LOWEST_RANK, DEFAULT_DAYS);
		}

		if (daysPerSlice == null || daysPerSlice.intValue() == 0) {
			daysPerSlice = DEFAULT_DAYS_PER_SLICE;
		}

		long todaySlice = sliceOffset(new Date(), daysPerSlice.intValue());
		long dateSlice;
		Integer rank;
		List<Integer> file;
		// get the dates in any order
		for (Date date : dateRank.keySet()) {
			dateSlice = sliceOffset(date, daysPerSlice.intValue());

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Offset [" + dateSlice + "] vs today [" + todaySlice + "]");
			}

			// if (dateSlice != todaySlice) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Slice does not match today");
			}

			file = files.get(Long.valueOf(dateSlice));

			if (file == null) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("File for [" + dateSlice + "], not found, creating it");
				}

				file = new ArrayList<Integer>();

				files.put(dateSlice, file);
			}

			rank = dateRank.get(date);

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Adding rank [" + rank.intValue() + "] to slice [" + dateSlice + "] file");
			}

			file.add(rank);
			// } else {
			//
			// }

		}

		if (LOGGER.isDebugEnabled()) {
			showFiles();
		}
	}

	@Command
	public void getDateRange(@Param(name = "slice") Long slice, @Param(name = "daysPerSlice") Integer daysPerSlice) {
		if (daysPerSlice == null || daysPerSlice.intValue() == 0) {
			daysPerSlice = DEFAULT_DAYS_PER_SLICE;
		}

		Calendar c = Calendar.getInstance();
		c.setTime(new Date(0));

		c.add(Calendar.DAY_OF_YEAR, slice.intValue() * daysPerSlice.intValue());

		Date startDate = new Date(c.getTimeInMillis());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Start date [" + SDF.format(startDate) + "]");
		}

		c.add(Calendar.DAY_OF_YEAR, daysPerSlice.intValue());

		Date endDate = new Date(c.getTimeInMillis());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("End date [" + SDF.format(endDate) + "]");
		}
	}

	@Command
	public void getSlices(@Param(name = "startDate") Date startDate, @Param(name = "endDate") Date endDate, @Param(name = "daysPerSlice") Integer daysPerSlice) {

		if (daysPerSlice == null || daysPerSlice.intValue() == 0) {
			daysPerSlice = DEFAULT_DAYS_PER_SLICE;
		}

		List<Long> dateRangeSlices = new ArrayList<Long>();

		long startDateSlice = sliceOffset(startDate, daysPerSlice.intValue());
		long endDateSlice = sliceOffset(endDate, daysPerSlice.intValue());

		for (long i = startDateSlice; i <= endDateSlice; i++) {
			dateRangeSlices.add(Long.valueOf(i));
		}

		if (LOGGER.isDebugEnabled()) {
			StringBuffer sb = new StringBuffer();

			for (Long slice : dateRangeSlices) {
				if (sb.length() != 0) {
					sb.append(",");
				}

				sb.append(slice.longValue());
			}

			LOGGER.debug(sb.toString());
		}
	}

	private long sliceOffset(Date date, int sliceDays) {
		return date.getTime() / (MILLIS_PER_DAY * (long) sliceDays);
	}

	private void showFiles() {
		StringBuffer sb = new StringBuffer();
		List<Integer> file;
		boolean first;

		for (Long slice : files.keySet()) {
			sb.setLength(0);
			first = true;

			file = files.get(slice);

			sb.append("File [");
			sb.append(slice.longValue());
			sb.append("]:");

			for (Integer rank : file) {
				if (!first) {
					sb.append(",");
				}

				sb.append(rank.intValue());
				first = false;
			}

			LOGGER.debug(sb.toString());
		}
	}
}
