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

/** Test stroke features. */
public class KanjiListTest
{
	/**
	 * Tests basic list functionality
	 * @throws Exception Any error
	 */
	@Test
	public void testBasic() throws Exception
	{
		KanjiList list = new KanjiList();
		KanjiInfo one = new KanjiInfo("x");
		one.addStroke(new InputStroke(0, 0, 100, 100));
		one.addStroke(new InputStroke(100, 0, 0, 100));
		one.finish();
		list.add(one);
		KanjiInfo two = new KanjiInfo("y");
		two.addStroke(new InputStroke(0, 0, 50, 50));
		two.addStroke(new InputStroke(100, 0, 0, 100));
		two.finish();
		list.add(two);
		
		assertEquals(0, list.getKanji(1).length);
		
		KanjiInfo[] both = list.getKanji(2);
		assertEquals(2, both.length);
		assertEquals(one, both[0]);
		assertEquals(two, both[1]);
	}
	
	/**
	 * Tests loading the list from the stored data
	 * @throws Exception Any error
	 */
	@Test
	public void testLoad() throws Exception
	{
		new KanjiList(KanjiList.class.getResourceAsStream("strokes-20100823.xml"));
	}
}
