/*
This file is part of leafdigital kanjirecog.

kanjirecog is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

kanjirecog is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with kanjirecog.  If not, see <http://www.gnu.org/licenses/>.

Copyright 2011 Samuel Marshall.
*/
package com.leafdigital.kanji;

import java.io.FileInputStream;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

import com.leafdigital.kanji.KanjiInfo.MatchAlgorithm;

/**
 * Analyses current performance based on the data stored in the database from
 * real users. This is a command-line tool. Run with the database password.
 */
public class AnalyseRecognition
{
	/**
	 * JDBC url for database (this probably shouldn't really be hardcoded but
	 * I doubt anyone but me will ever run it).
	 */
	private final static String JDBC_URL = "jdbc:postgresql://localhost/kanji";

	/**
	 * Shouldn't be hardcoded either.
	 */
	private final static String JDBC_USER = "kanji";

	/**
	 * Basic query (without WHERE clause)
	 */
	private final static String BASIC_QUERY = "SELECT drawing, kanji FROM attempts";

	/**
	 * Do it in consistent order.
	 */
	private final static String ORDER_BY = " ORDER BY addtime";

	/**
	 * Array of default where clauses that can be accessed using =0 or =1 etc
	 * on the commandline.
	 */
	private final static String[] DEFAULT_WHERE_CLAUSES =
	{
		"clientname like 'leafdigital %'",
		"clientname like 'leafdigital %' and ranking=1 and algo='STRICT'",
		"clientname like 'leafdigital %' and algo='FUZZY'",
		"clientname like 'leafdigital %' and algo='FUZZY_1OUT'"
	};

	/**
	 * @param args Command-line arguments; first one must be db password, second
	 *   is optional conditions for use in WHERE clause, third is optional
	 *   match algorithm (otherwise all are run)
	 * @throws Exception Any error
	 */
	public static void main(String[] args) throws Exception
	{
		String where = null;
		if(args.length > 1)
		{
			if(args[1].matches("^=[0-9]{1,5}$"))
			{
				int defaultWhere = Integer.parseInt(args[1].substring(1));
				where = DEFAULT_WHERE_CLAUSES[defaultWhere];
			}
			else
			{
				where = args[1];
			}
		}

		MatchAlgorithm algo = null;
		if(args.length > 2)
		{
			algo = MatchAlgorithm.valueOf(args[2]);
		}

		new AnalyseRecognition().run(args[0], where, algo);
	}

	/**
	 * @param password Password
	 * @param where Optional where clause (null if none), not including "WHERE"
	 * @param soloAlgo Match algorithm or null for all
	 * @throws Exception Any error
	 */
	private void run(String password, String where, MatchAlgorithm soloAlgo)
		throws Exception
	{
		// Get database connection
		Class.forName("org.postgresql.Driver");
		Properties props = new Properties();
		props.setProperty("user", JDBC_USER);
		props.setProperty("password", password);
		Connection c = null;
		Statement s = null;
		ResultSet rs = null;
		try
		{
			c = DriverManager.getConnection(JDBC_URL, props);

			// Work out query, including parameter 1 as WHERE clause if included
			String query = BASIC_QUERY;
			if(where != null)
			{
				query += " WHERE " + where;
			}
			query += ORDER_BY;

			// Run query
			s = c.createStatement();
			rs = s.executeQuery(query);

			// Start
			start();

			// Loop through all results
			while(rs.next())
			{
				process(rs.getString("drawing"), rs.getString("kanji"), soloAlgo);
			}

			// Handle result when finihed
			finish();
		}
		finally
		{
			if(rs != null)
			{
				rs.close();
			}
			if(s != null)
			{
				s.close();
			}
			if(c != null)
			{
				c.close();
			}
		}
	}

	private final static int MAX_QUEUE_SIZE = 10;

	private int processed, added;
	private KanjiList list;

	private ExecutorService threadPool;

	private Map<MatchAlgorithm, AlgoResults> results;

	private long startTime;

	/**
	 * Tracks results for a specific algorithm.
	 */
	private class AlgoResults
	{
		private MatchAlgorithm algo;
		private Map<Integer, Integer> rankings = new TreeMap<Integer, Integer>();
		private int failures = 0;
		private int total = 0;
		private double cumulative = 0;

		private AlgoResults(MatchAlgorithm algo)
		{
			this.algo = algo;
		}

		/**
		 * Adds a ranked result.
		 * @param ranking Ranking (starting from 1)
		 */
		private void addRanking(int ranking)
		{
			Integer existing = rankings.get(ranking);
			if(existing == null)
			{
				rankings.put(ranking, 1);
			}
			else
			{
				rankings.put(ranking, existing+1);
			}
			total++;
		}

		/**
		 * Adds a failed result.
		 * @param info
		 */
		private void addFailure(KanjiInfo info)
		{
			failures++;
			total++;
		}

		/**
		 * Displays results to standard out.
		 */
		private void display()
		{
			// Work out the max ranking that is actually displayed by the Android
			// app. Anything higher counts as a failure.
			int max = 24;
			if(algo == MatchAlgorithm.STRICT)
			{
				max = 7;
			}

			// Display ranking counts
			for(int i=1; i<=max; i++)
			{
				Integer count = rankings.get(i);
				if(count == null)
				{
					count = 0;
				}
				display(String.format("%4d", i), count);
			}

			// Count everything above the max and display alongside failures
			int maxPlus = 0;
			int totalRanking = 0;
			for(Map.Entry<Integer, Integer> ranking : rankings.entrySet())
			{
				if(ranking.getKey() > max)
				{
					maxPlus += ranking.getValue();
					// Count all fails as rank 50
					totalRanking += 50 * ranking.getValue();
				}
				else
				{
					totalRanking += ranking.getKey() * ranking.getValue();
				}
			}
			maxPlus += failures;
			display("Fail", maxPlus);

			// Show summary
			double averageRanking = (double)totalRanking / (total - failures);
			System.out.println();
			System.out.println(String.format(
				"Weighted average ranking %.2f (including %.1f%% failures at rank 50)",
				averageRanking, 100.0 * (double)maxPlus / (double)total));
		}

		/**
		 * Displays a line of data with count, percentage, and cumulative
		 * percentage.
		 * @param index Text for 4-character index at left
		 * @param count Number of counts for this line
		 */
		private void display(String index, int count)
		{
			double percentage = 100.0 * (double)count / (double)total;
			cumulative += percentage;
			System.out.println(String.format("%4s %7d %7.1f%% %7.1f%%",
				index, count, percentage, cumulative));
		}
	}

	/**
	 * Starts analysis process.
	 * @throws Exception
	 */
	private synchronized void start() throws Exception
	{
		processed = 0;
		added = 0;
		list = new KanjiList(new FileInputStream("data/strokes-20100823.xml"));
		results = new TreeMap<MatchAlgorithm, AlgoResults>();

		startTime = System.currentTimeMillis();

		// Start the thread pool
		threadPool = Executors.newFixedThreadPool(
			Runtime.getRuntime().availableProcessors());
	}

	/**
	 * Processes a single kanji drawing.
	 * @param drawing Drawing
	 * @param kanji Resulting selected kanji
	 * @param soloAlgo Match algorithm or null for all
	 * @throws InterruptedException Probably shouldn't really happen
	 */
	private synchronized void process(final String drawing, final String kanji,
		final MatchAlgorithm soloAlgo)
		throws InterruptedException
	{
		while(added > processed + MAX_QUEUE_SIZE)
		{
			wait();
		}
		added++;
		threadPool.execute(new Runnable()
		{
			@Override
			public void run()
			{
				// Create drawing info
				KanjiInfo drawingInfo = new KanjiInfo(kanji, drawing);

				// Find actual number of strokes
				int actualStrokes = list.find(kanji).getStrokeCount();

				// Decide which algorithms to use based on stroke count
				List<MatchAlgorithm> algorithms = new LinkedList<MatchAlgorithm>();
				for(MatchAlgorithm algo : MatchAlgorithm.values())
				{
					if(algo.getOut() == Math.abs(actualStrokes - drawingInfo.getStrokeCount()))
					{
						algorithms.add(algo);
					}
				}

				// Process for each algorithm
				for(MatchAlgorithm algo : algorithms)
				{
					if(soloAlgo == null || soloAlgo == algo)
					{
						process(drawingInfo, algo);
					}
				}

				synchronized(AnalyseRecognition.this)
				{
					processed++;
					System.err.print("Processed: " + processed + "\r");
					AnalyseRecognition.this.notifyAll();
				}
			}
		});

	}

	/**
	 * Processes a single kanji drawing with a single algorithm.
	 * @param drawingInfo Drawing
	 * @param algo Algorithm to use
	 */
	private void process(KanjiInfo drawingInfo, MatchAlgorithm algo)
	{
		KanjiMatch[] matches = list.getTopMatches(drawingInfo, algo, null);

		synchronized(AnalyseRecognition.this)
		{
			AlgoResults algoResults = results.get(algo);
			if(algoResults == null)
			{
				algoResults = new AlgoResults(algo);
				results.put(algo, algoResults);
			}

			for(int i=0; i<matches.length; i++)
			{
				if(matches[i].getKanji().getKanji().equals(drawingInfo.getKanji()))
				{
					algoResults.addRanking(i+1);
					return;
				}
			}

			algoResults.addFailure(drawingInfo);
		}
	}

	/**
	 * Called when the analysis run finishes. Displays all results.
	 * @throws Exception Any error
	 */
	private void finish() throws Exception
	{
		threadPool.shutdown();
		threadPool.awaitTermination(1, TimeUnit.DAYS);
		long endTime = System.currentTimeMillis();
		System.err.println();
		for(Map.Entry<MatchAlgorithm,AlgoResults> entry : results.entrySet())
		{
			// Blank line before algorithm name and underline, then blank line
			String algoName = entry.getKey().toString();
			System.out.println();
			System.out.println(algoName);
			for(int i=0; i<algoName.length(); i++)
			{
				System.out.print("=");
			}
			System.out.println();
			System.out.println();

			// Print results
			entry.getValue().display();
		}
		System.out.println();
		System.out.println("PERFORMANCE");
		System.out.println("===========");
		System.out.println();
		double msPerCharacter = (double)(endTime - startTime) / (double)processed;
		System.out.println(String.format("%.1f ms / character",
			msPerCharacter));
	}
}
