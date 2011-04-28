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

/**
 * Interface for a method that can compare different kanji.
 */
public interface KanjiComparer
{
	/**
	 * Initialises the comparer. Should be called immediately after the
	 * constructor.
	 * @param info Kanji that the user drew
	 */
	public void init(KanjiInfo info);

	/**
	 * Compares against the given other kanji.
	 * @param other Other kanji
	 * @return Score in range 0 to 100
	 */
	public float getMatchScore(KanjiInfo other);
}
