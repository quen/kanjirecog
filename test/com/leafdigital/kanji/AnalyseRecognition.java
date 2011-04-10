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
		"clientname like 'leafdigital %' and ranking=1 and algo='STRICT'"
	};

	/**
	 * @param args Command-line arguments; first one must be db password, second
	 *   is optional conditions for use in WHERE clause
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

		new AnalyseRecognition().run(args[0], where);
	}

	/**
	 * @param password Password
	 * @param where Optional where clause (null if none), not including "WHERE"
	 * @throws Exception Any error
	 */
	private void run(String password, String where) throws Exception
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
				process(rs.getString("drawing"), rs.getString("kanji"));
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

	private int count;
	private KanjiList list;

	private Map<MatchAlgorithm, AlgoResults> results;

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
				"Summary: avg ranking %.2f / fail %.1f%%",
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
	private void start() throws Exception
	{
		count = 0;
		list = new KanjiList(new FileInputStream("data/strokes-20100823.xml"));
		results = new TreeMap<MatchAlgorithm, AlgoResults>();
	}

	/**
	 * Processes a single kanji drawing.
	 * @param drawing Drawing
	 * @param kanji Resulting selected kanji
	 */
	private void process(String drawing, String kanji)
	{
		// This could run much faster if I multi-cored it, but I'm currently too
		// lazy. Maybe when the database gets bigger... (The best approach would
		// be to have a list of tasks and a number of worker threads that process
		// them; this method would add tasks to the list.)

		// Create drawing info
		KanjiInfo drawingInfo = new KanjiInfo(kanji, drawing);

		// Find actual number of strokes
		int actualStrokes = list.find(kanji).getStrokeCount();

		// Decide which algorithms to use based on stroke count
		MatchAlgorithm[] algorithms;
		if(actualStrokes == drawingInfo.getStrokeCount())
		{
			algorithms = new MatchAlgorithm[] { MatchAlgorithm.STRICT, MatchAlgorithm.FUZZY };
		}
		else if(Math.abs(actualStrokes - drawingInfo.getStrokeCount()) == 1)
		{
			algorithms = new MatchAlgorithm[] { MatchAlgorithm.FUZZY_1OUT };
		}
		else if(Math.abs(actualStrokes - drawingInfo.getStrokeCount()) == 2)
		{
			algorithms = new MatchAlgorithm[] { MatchAlgorithm.FUZZY_1OUT };
		}
		else
		{
			algorithms = new MatchAlgorithm[0];
		}

		// Process for each algorithm
		for(MatchAlgorithm algo : algorithms)
		{
			process(drawingInfo, algo);
		}

		count++;
		System.err.print("Processed: " + count + "\r");
	}

	/**
	 * Processes a single kanji drawing with a single algorithm.
	 * @param drawingInfo Drawing
	 * @param algo Algorithm to use
	 */
	private void process(KanjiInfo drawingInfo, MatchAlgorithm algo)
	{
		AlgoResults algoResults = results.get(algo);
		if(algoResults == null)
		{
			algoResults = new AlgoResults(algo);
			results.put(algo, algoResults);
		}

		KanjiMatch[] matches = list.getTopMatches(drawingInfo, algo, null);
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

	/**
	 * Called when the analysis run finishes. Displays all results.
	 */
	private void finish()
	{
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
	}
}
