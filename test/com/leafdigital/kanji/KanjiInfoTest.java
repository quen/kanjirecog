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

import org.junit.Test;

/** Test stroke features. */
public class KanjiInfoTest
{
	/**
	 * Tests basic functionality
	 * @throws Exception Any error
	 */
	@Test
	public void testBasic() throws Exception
	{
		KanjiInfo one = new KanjiInfo("x");
		one.addStroke(new InputStroke(0, 0, 100, 100));
		one.addStroke(new InputStroke(100, 0, 0, 100));
		one.finish();
		
		assertEquals("x", one.getKanji());
		assertEquals(2, one.getStrokeCount());
		assertEquals("[0,0:255,255]", one.getStroke(0).toString());
		assertEquals("[255,0:0,255]", one.getStroke(1).toString());
		
		assertEquals("00,00-ff,ff:ff,00-00,ff", one.getFullSummary());
	}
	
	/**
	 * Tests that the summarised versions are the same in terms of directions
	 * @throws Exception
	 */
	@Test
	public void testFullSummary() throws Exception
	{
		// Load all kanji
		float totalScore = 0f;
		int totalCount = 0;
		for(KanjiInfo kanji : KanjiVgLoaderTest.getAll())
		{
			// Get the summary string, and make a new kanji with it
			KanjiInfo loaded = new KanjiInfo(kanji.getKanji(), kanji.getFullSummary());
			
			// Check the directions are basically the same (may be slight differences
			// because rounding could push the '<0.1' type comparisons either way)
			float matchScore = loaded.getMatchScore(kanji, 
				KanjiInfo.MatchAlgorithm.STRICT);
			assertTrue(matchScore > 94f);
			totalScore += matchScore;
			totalCount++;
		}
		
		// Check on average the similarity was extremely high
		assertTrue(totalScore / (float)totalCount > 99f);
	}
	
	/**
	 * Tests the two-digit string converter.
	 */
	@Test
	public void testGetTwoDigitHexInt()
	{
		assertEquals(0, KanjiInfo.getTwoDigitHexInt("00", 0));
		assertEquals(1, KanjiInfo.getTwoDigitHexInt("01", 0));
		assertEquals(16, KanjiInfo.getTwoDigitHexInt("10", 0));
		assertEquals(9*16+9, KanjiInfo.getTwoDigitHexInt("99", 0));
		assertEquals(255, KanjiInfo.getTwoDigitHexInt("ff", 0));
		assertEquals(9*16+9, KanjiInfo.getTwoDigitHexInt("blah99blah", 4));
	}
}
