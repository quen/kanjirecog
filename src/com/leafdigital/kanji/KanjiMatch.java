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
 * A match from search results.
 */
public class KanjiMatch implements Comparable<KanjiMatch>
{
	private KanjiInfo kanji;
	private float score;
	
	/**
	 * @param kanji Kanji
	 * @param score Match score (higher is better)
	 */
	KanjiMatch(KanjiInfo kanji, float score)
	{
		this.kanji = kanji;
		this.score = score;
	}
	
	/** 
	 * @return Score
	 */
	public float getScore()
	{
		return score;
	}
	
	/** 
	 * @return Kanji info
	 */
	public KanjiInfo getKanji()
	{
		return kanji;
	}

	@Override
	public int compareTo(KanjiMatch o)
	{
		if(score > o.score)
		{
			return -1;
		}
		else if(score < o.score)
		{
			return 1;
		}
		else
		{
			return kanji.getKanji().compareTo(o.kanji.getKanji());
		}
	}
}
