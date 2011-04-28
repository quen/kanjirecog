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
 * Compares entered strokes with other kanji based on the number of points
 * (start or end) in different areas of the kanji.
 * This comparer is very fast, but works very poorly; not suitable for any use.
 */
public class DotsComparer implements KanjiComparer
{
	final static int NUM_RANGES = 5;

	private int[] countsX, countsY;

	/**
	 * Initialises with given drawn kanji.
	 * @param info Drawn kanji
	 */
	@Override
	public void init(KanjiInfo info)
	{
		countsX = new int[NUM_RANGES];
		countsY = new int[NUM_RANGES];
		fillCounts(countsX, countsY, info);
	}

	/**
	 * Compares against the given other kanji.
	 * @param other Other kanji
	 * @return Score in range 0 to 100
	 */
	@Override
	public float getMatchScore(KanjiInfo other)
	{
		// Get counts for other kanji
		int[] otherX, otherY;
		otherX = new int[NUM_RANGES];
		otherY = new int[NUM_RANGES];
		fillCounts(otherX, otherY, other);

		// Compare the two count arrays and use that as score
		float score = 0f;
		score +=  compareArrays(countsX, otherX, false);
		score += compareArrays(countsY, otherY, false);

		score = score * 100f / 2f;
		return score;
	}

	/**
	 * Calculates start and end position counts.
	 * @param countsX X counts array
	 * @param countsY Y counts array
	 * @param info Kanji
	 */
	static void fillCounts(int[] countsX, int[] countsY, KanjiInfo info)
	{
		for(int i=0; i<info.getStrokeCount(); i++)
		{
			Stroke s = info.getStroke(i);
			countsX[(s.getStartX() * NUM_RANGES) >> 8]++;
			countsY[(s.getStartY() * NUM_RANGES) >> 8]++;
			countsX[(s.getEndX() * NUM_RANGES) >> 8]++;
			countsY[(s.getEndY() * NUM_RANGES) >> 8]++;
		}
	}

	/**
	 * Compares two arrays of length NUM_RANGES. Score 2 points for each exact
	 * match (where an item from counts1[4] matches one in counts2[4]) and 1
	 * point for each neighbour match. Points are then scaled out of maximum
	 * possible (given any difference in counts).
	 * @param counts1 First array
	 * @param counts2 Second array
	 * @param wrap True if the last item is considered a neighbour of the first
	 * @return Score from 0 to 1.
	 */
	static float compareArrays(int[] counts1, int[] counts2, boolean wrap)
	{
		int n = counts1.length;
		int score = 0;
		int[] left1 = new int[n], left2 = new int[n];
		int total1 = 0, total2 = 0;

		// Remove exact matches
		for(int i=0; i<n; i++)
		{
			int count1 = counts1[i], count2 = counts2[i];
			total1 += count1;
			total2 += count2;
			if(count1 > count2)
			{
				score += count2;
				left1[i] = count1 - count2;
				left2[i] = 0;
			}
			else if(count2 > count1)
			{
				score += count1;
				left1[i] = 0;
				left2[i] = count2 - count1;
			}
			else // equal
			{
				score += count1;
				// Set it to zero - but don't bother because that's the default
			}
		}

		// If there are none in either, give it a match
		if(total1 == 0 && total2 == 0)
		{
			return 1f;
		}

		// Double score (because you get 2 points for exact matches)
		score *= 2;

		// Loop through all matches to match against neighbours
		for(int i=0; i<n; i++)
		{
			// Any left?
			int remaining = left1[i];
			if(remaining == 0)
			{
				continue;
			}

			// Try either side
			if(i > 0)
			{
				int before = left2[i-1];
				if(before >= remaining)
				{
					score += remaining;
					left2[i-1] = before - remaining;
					remaining = 0;
				}
				else if(before > 0)
				{
					score += before;
					left2[i-1] = 0;
					remaining -= before;
				}
			}
			else if(wrap)
			{
				int before = left2[n-1];
				if(before >= remaining)
				{
					score += remaining;
					left2[n-1] = before - remaining;
					remaining = 0;
				}
				else if(before > 0)
				{
					score += before;
					left2[n-1] = 0;
					remaining -= before;
				}
			}

			if(remaining > 0)
			{
				if(i < n-1)
				{
					int after = left2[i+1];
					if(after >= remaining)
					{
						score += remaining;
						left2[i+1] = after - remaining;
						remaining = 0;
					}
					else if(after > 0)
					{
						score += after;
						left2[i+1] = 0;
						remaining -= after;
					}
				}
				else if(wrap)
				{
					int after = left2[0];
					if(after >= remaining)
					{
						score += remaining;
						left2[0] = after - remaining;
						remaining = 0;
					}
					else if(after > 0)
					{
						score += after;
						left2[0] = 0;
						remaining -= after;
					}
				}
			}
		}

		// Max score is a score where everything matches every other thing, except
		// that you have leftovers equal to the difference in counts
		int maxScore = Math.min(total1, total2) * 2;
		if(maxScore==0)
		{
			return 0f;
		}

		// Return scaled score
		return (float)score / (float)maxScore;
	}
}
