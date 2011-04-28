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

import static com.leafdigital.kanji.DotsComparer.compareArrays;

/**
 * Compares entered strokes with other kanji using two factors:
 * - The number of strokes of different start/end locations in X and Y axes
 * - The number of strokes of different directions
 * This algorithm is a lot faster than the fuzzy algorithm, but is currently
 * distinctly worse.
 */
public class SpansComparer implements KanjiComparer
{
	final static int NUM_RANGES = 5;
	final static int NUM_DIRECTIONS = 4;

	private final static float DIRECTION_IMPORTANCE = 0.6f;

	private int[][] xThis, yThis;
	private int[] directionThis;

	static int[][] makeSpansArray()
	{
		int[][] spans = new int[NUM_RANGES][];
		for(int i=0; i<NUM_RANGES; i++)
		{
			spans[i] = new int[NUM_RANGES];
		}
		return spans;
	}

	static void fillSpans(int[][] xSpans, int[][] ySpans, KanjiInfo info)
	{
		for(int i=0; i<info.getStrokeCount(); i++)
		{
			Stroke s = info.getStroke(i);
			int startX = (s.getStartX() * NUM_RANGES) >> 8;
			int startY = (s.getStartY() * NUM_RANGES) >> 8;
			int endX = (s.getEndX() * NUM_RANGES) >> 8;
			int endY = (s.getEndY() * NUM_RANGES) >> 8;

			xSpans[startX][endX]++;
			xSpans[endX][startX]++; // Keeping this is a waste of time
			ySpans[startY][endY]++;
			ySpans[endY][startY]++;
		}
	}

	/**
	 * Initialises with given drawn kanji.
	 * @param info Drawn kanji
	 */
	@Override
	public void init(KanjiInfo info)
	{
		xThis = makeSpansArray();
		yThis = makeSpansArray();
		fillSpans(xThis, yThis, info);
		directionThis = new int[NUM_DIRECTIONS];
		fillDirections(directionThis, info);
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
		int[][] xOther = makeSpansArray();
		int[][] yOther = makeSpansArray();
		fillSpans(xOther, yOther, other);

		// Compare the two count arrays and use that as score
		float score = 0f;
		for(int i=0; i<NUM_RANGES; i++)
		{
			score += compareArrays(xThis[i], xOther[i], false);
			score += compareArrays(yThis[i], yOther[i], false);
		}
		score = score * 100f / (2*NUM_RANGES);

		// Get directions for other kanji
		int[] directionOther = new int[NUM_DIRECTIONS];
		fillDirections(directionOther, other);
		float directionScore = compareArrays(directionThis, directionOther, true) * 100f;

		// Return balance of direction and span score
		return score * (1f-DIRECTION_IMPORTANCE) + directionScore * DIRECTION_IMPORTANCE;
	}

	static void fillDirections(int[] directions, KanjiInfo info)
	{
		for(int i=0; i<info.getStrokeCount(); i++)
		{
			Stroke s = info.getStroke(i);
			switch(s.getDirectionNoThreshold())
			{
			case W:
			case E:
				// This direction: -
				directions[0]++;
				break;
			case NW:
			case SE:
				// This direction: \
				directions[1]++;
				break;
			case N:
			case S:
				// This direction: |
				directions[2]++;
				break;
			case NE:
			case SW:
				// This direction: /
				directions[3]++;
				break;
			}
		}
	}
}
