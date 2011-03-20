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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Test fuzzy comparer. */
public class FuzzyComparerTest
{
	/**
	 * Set this flag on to enable timing comparison by testing 100 characters
	 * instead of just 5. On my system this currently takes around 20.4 seconds.
	 */
	private static boolean DEBUG = false;
	
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
			KanjiList.class.getResourceAsStream("strokes-20100823.xml"));
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
				list.getTopMatches(big, KanjiInfo.MatchAlgorithm.FUZZY);
			assertEquals(big.getKanji(), matches[0].getKanji().getKanji());
		}
	}
}
