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

import com.leafdigital.kanji.Stroke.Direction;
import com.leafdigital.kanji.Stroke.Location;

/**
 * Compares entered strokes with other kanji using slightly fuzzy logic.
 */
public class StrictComparer implements KanjiComparer
{
	private final static float STROKE_DIRECTION_WEIGHT = 1.0f;
	private final static float MOVE_DIRECTION_WEIGHT = 0.8f;
	private final static float STROKE_LOCATION_WEIGHT = 0.6f;

	private final static float CLOSE_WEIGHT = 0.7f;

	private Location[] drawnStarts, drawnEnds;
	private Direction[] drawnDirections, drawnMoves;

	/**
	 * Initialises with given drawn kanji.
	 * @param info Drawn kanji
	 */
	@Override
	public void init(KanjiInfo info)
	{
		drawnStarts = info.getStrokeStarts();
		drawnEnds = info.getStrokeEnds();
		drawnDirections = info.getStrokeDirections();
		drawnMoves = info.getMoveDirections();
	}

	/**
	 * Compares against the given other kanji.
	 * @param other Other kanji
	 * @return Score in range 0 to 100
	 */
	@Override
	public float getMatchScore(KanjiInfo other)
	{
		Location[] otherStarts = other.getStrokeStarts(),
			otherEnds = other.getStrokeEnds();
		Direction[] otherDirections = other.getStrokeDirections(),
			otherMoves = other.getMoveDirections();

		if(otherStarts.length != drawnStarts.length)
		{
			throw new IllegalArgumentException(
				"Can only compare with same match length");
		}

		float score = 0;
		for(int i=0; i<drawnStarts.length; i++)
		{
			// Stroke direction
			if(drawnDirections[i] == otherDirections[i])
			{
				score += STROKE_DIRECTION_WEIGHT;
			}
			else if(drawnDirections[i].isClose(otherDirections[i]))
			{
				score += STROKE_DIRECTION_WEIGHT * CLOSE_WEIGHT;
			}

			// Move direction
			if(i>0)
			{
				if(drawnMoves[i-1] == otherMoves[i-1])
				{
					score += MOVE_DIRECTION_WEIGHT;
				}
				else if(drawnMoves[i-1].isClose(otherMoves[i-1]))
				{
					score += MOVE_DIRECTION_WEIGHT * CLOSE_WEIGHT;
				}
			}

			// Start and end locations
			if(drawnStarts[i] == otherStarts[i])
			{
				score += STROKE_LOCATION_WEIGHT;
			}
			else if(drawnStarts[i].isClose(otherStarts[i]))
			{
				score += STROKE_LOCATION_WEIGHT * CLOSE_WEIGHT;
			}
			if(drawnEnds[i] == otherEnds[i])
			{
				score += STROKE_LOCATION_WEIGHT;
			}
			else if(drawnEnds[i].isClose(otherEnds[i]))
			{
				score += STROKE_LOCATION_WEIGHT * CLOSE_WEIGHT;
			}
		}

		float max = drawnStarts.length * (STROKE_DIRECTION_WEIGHT
			+ 2 * STROKE_LOCATION_WEIGHT)
			+	(drawnStarts.length - 1) * MOVE_DIRECTION_WEIGHT;

		return 100.0f * score / max;
	}
}
