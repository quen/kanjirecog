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

import static org.junit.Assert.*;

import java.io.FileInputStream;

import org.junit.Test;

/** Test fuzzy comparer. */
public class DotsComparerTest
{
	/**
	 * Set this flag on to enable timing comparison by testing 100 characters
	 * instead of just 5.
	 */
	private static boolean DEBUG = false;

	/**
	 * Test the point fill function.
	 */
	@Test
	public void testFillCounts()
	{
		// Simple example
		KanjiInfo info = new KanjiInfo("?");
		info.addStroke(new InputStroke(0, 0, 1, 0));
		info.finish();
		int[] x = new int[5], y = new int[5];
		DotsComparer.fillCounts(x, y, info);
		assertArrayEquals(new int[] { 1, 0, 0, 0, 1 }, x);
		assertArrayEquals(new int[] { 0, 0, 2, 0, 0 }, y);
	}

	/**
	 * Test the array comparison function
	 */
	@Test
	public void testCompareArrays()
	{
		// Basic equal case
		assertEquals(1.0, DotsComparer.compareArrays(
			new int[] { 1, 0, 7, 4, 0 },
			new int[] { 1, 0, 7, 4, 0 }, false), 0.00001);

		// Shift one (various locations)
		assertEquals(0.5, DotsComparer.compareArrays(
			new int[] { 1, 0, 0, 0, 0 },
			new int[] { 0, 1, 0, 0, 0 }, false), 0.00001);
		assertEquals(0.5, DotsComparer.compareArrays(
			new int[] { 0, 1, 0, 0, 0 },
			new int[] { 1, 0, 0, 0, 0 }, false), 0.00001);
		assertEquals(0.5, DotsComparer.compareArrays(
			new int[] { 0, 0, 0, 0, 1 },
			new int[] { 0, 0, 0, 1, 0 }, false), 0.00001);
		assertEquals(0.5, DotsComparer.compareArrays(
			new int[] { 0, 0, 0, 1, 0 },
			new int[] { 0, 0, 0, 0, 1 }, false), 0.00001);

		// Shift some left over
		assertEquals(0.25, DotsComparer.compareArrays(
			new int[] { 2, 0, 0, 0, 0 },
			new int[] { 0, 1, 1, 0, 0 }, false), 0.00001);

		// Ensure shift can't be used twice
		assertEquals(0.5, DotsComparer.compareArrays(
			new int[] { 0, 1, 0, 1, 0 },
			new int[] { 0, 0, 1, 0, 0 }, false), 0.00001);

		// Some equal, some shift
		assertEquals(0.75, DotsComparer.compareArrays(
			new int[] { 1, 0, 1, 0, 0 },
			new int[] { 0, 1, 1, 0, 0 }, false), 0.00001);

		// One equal, one left over (both ways)
		assertEquals(1.0, DotsComparer.compareArrays(
			new int[] { 1, 0, 1, 0, 0 },
			new int[] { 0, 0, 1, 0, 0 }, false), 0.00001);
		assertEquals(1.0, DotsComparer.compareArrays(
			new int[] { 0, 0, 1, 0, 0 },
			new int[] { 1, 0, 1, 0, 0 }, false), 0.00001);

		// Arrays filled with zeros
		assertEquals(1.0, DotsComparer.compareArrays(
			new int[] { 0, 0, 0, 0, 0 },
			new int[] { 0, 0, 0, 0, 0 }, false), 0.00001);

		// First array has zeros
		assertEquals(0.0, DotsComparer.compareArrays(
			new int[] { 0, 0, 0, 0, 0 },
			new int[] { 0, 0, 0, 0, 1 }, false), 0.00001);

		// Second array has zeros
		assertEquals(0.0, DotsComparer.compareArrays(
			new int[] { 0, 0, 0, 0, 1 },
			new int[] { 0, 0, 0, 0, 0 }, false), 0.00001);
	}

	/**
	 * Tests basic functionality
	 * @throws Exception Any error
	 */
	@Test
	public void testBigMatch() throws Exception
	{
		// This compares the first 5 20-stroke kanji characters against all the
		// others and checks that they match themselves. In addition to correctness
		// checking it can be used for timing comparison (increasing the limit may
		// give more stable results).
		KanjiList list = new KanjiList(
			new FileInputStream("data/strokes-20100823.xml"));
		KanjiInfo[] all20 = list.getKanji(20);
		for(int i=0; i<all20.length && i<(DEBUG ? 100 : 5); i++)
		{
			KanjiInfo big = all20[i];
			if(DEBUG)
			{
				System.err.print(big.getKanji());
				if(i%10 == 9)
				{
					System.err.println();
				}
			}
			KanjiMatch[] matches =
				list.getTopMatches(big, KanjiInfo.MatchAlgorithm.DOTS, null);
			assertEquals(big.getKanji(), matches[0].getKanji().getKanji());
		}
	}
}
