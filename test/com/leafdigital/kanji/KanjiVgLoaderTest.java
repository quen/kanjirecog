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

import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.util.TreeSet;

import org.junit.Test;

/** Test kanji reading from KanjiVG file. */
public class KanjiVgLoaderTest
{
	private static KanjiVgLoader reader;
	private static KanjiInfo[] info;
	private static boolean DEBUG = false;
	
	/**
	 * Loads all kanji from data file.
	 * @return All kanji
	 * @throws Exception
	 */
	public static KanjiInfo[] getAll() throws Exception
	{
		if(info == null)
		{
			reader = new KanjiVgLoader(
				new FileInputStream("data/kanjivg-20100823.xml"));
			info = reader.loadKanji();
		}
		return info;
	}
	
	/**
	 * Test loading the file
	 * @throws Exception Any error
	 */
	@Test
	public void testBasic() throws Exception
	{
		KanjiInfo[] info = getAll();
		
		if(DEBUG)
		{
			System.err.println(info.length);
			System.err.println(reader.getWarnings().length);
			for(String warning : reader.getWarnings())
			{
				System.err.println(warning);
			}
		}
		
		assertTrue(info.length >= 6366);
		assertTrue(reader.getWarnings().length <= 26);
	}
	
	/**
	 * Test loading combined with list feature.
	 * @throws Exception
	 */
	@Test
	public void testWithList() throws Exception
	{
		// Add all the kanji to a list
		KanjiList list = new KanjiList();
		for(KanjiInfo kanji : getAll())
		{
			list.add(kanji);
		}
		
		// Get all the entries and sort by directions
		int duplicates = 0;
		for(int count=1; count<50; count++)
		{
			TreeSet<String> strings = new TreeSet<String>();
			KanjiInfo[] info = list.getKanji(count);
			for(KanjiInfo kanji : info)
			{
				strings.add(kanji.getAllDirections() + " " + kanji.getKanji());
			}
			
			String last = null;
			String lastArrows = null;
			for(String string : strings)
			{
				String arrows = string.substring(0, string.indexOf(' '));
				if(arrows.equals(lastArrows))
				{
					if(DEBUG)
					{
						if(last != null)
						{
							System.err.println();
							System.err.println(last);
						}
						System.err.println(string);
					}
					duplicates++;
				}
				else
				{
					last = string;
				}
				lastArrows = arrows;
			}
		}
		
		assertTrue(duplicates <= 17);		
	}
}
