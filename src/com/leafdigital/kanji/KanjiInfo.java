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

import java.io.*;
import java.util.LinkedList;

import com.leafdigital.kanji.Stroke.*;

/** 
 * Holds stroke info about a single kanji.
 */
public class KanjiInfo
{
	/** 
	 * Algorithm used for comparing kanji. 
	 */
	public enum MatchAlgorithm
	{
		/** 
		 * Accurate, fast, but strict algorithm (requires precise stroke count
		 * and order).
		 */
		STRICT, 
		/**
		 * Fuzzy matching algorithm  which allows arbitrary stroke order. Very slow.
		 */
		FUZZY,
		/**
		 * Fuzzy matching algorithm  which allows arbitrary stroke order; with
		 * either +1 or -1 stroke count (does not include =). Even slower.
		 */
		FUZZY_1OUT,
		/**
		 * Fuzzy matching algorithm  which allows arbitrary stroke order; with
		 * either +2 or -2 stroke count. Also slow
		 */
		FUZZY_2OUT
	};
	
	private String kanji;
	private LinkedList<InputStroke> loadingStrokes;
	private Stroke[] strokes;
	private Direction[] strokeDirections, moveDirections;
	private Location[] strokeStarts, strokeEnds;
	
	private FuzzyComparer fuzzyComparer;
	
	/**
	 * @param kanji Kanji character (should be a single character, but may be
	 *   a UTF-16 surrogate pair)
	 */
	public KanjiInfo(String kanji)
	{
		this.kanji = kanji;
		loadingStrokes = new LinkedList<InputStroke>();
	}
	
	/**
	 * @param kanji Kanji character (should be a single character, but may be
	 *   a UTF-16 surrogate pair)
	 * @param full Full summary string (in {@link #getFullSummary()} format)
	 * @throws IllegalArgumentException If strokes string has invalid format
	 */
	KanjiInfo(String kanji, String full) throws IllegalArgumentException
	{
		this.kanji = kanji;

		int count = (full.length()+1) / 12;
		if((count * 12 - 1) != full.length())
		{
			throw new IllegalArgumentException("Invalid full (" + full 
				+ ") for kanji (" + kanji + ")");
		}
		
		try
		{
			strokes = new Stroke[count];
			int offset = 0;
			for(int i=0; i<count; i++)
			{
				if(i != 0)
				{
					offset++; // Skip colon
				}
				
				strokes[i] = new Stroke(
					getTwoDigitHexInt(full, offset),
					getTwoDigitHexInt(full, offset+3),
					getTwoDigitHexInt(full, offset+6),
					getTwoDigitHexInt(full, offset+9));
				offset+=11;
			}
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
			throw new IllegalArgumentException("Invalid summary(" + full
				+ ") for kanji (" + kanji + ")");
		}
		
		findDirections();
	}
	
	/**
	 * Converts a two-digit, lowercase hex string to an integer. (This is a lot
	 * faster than doing a substring and Integer.parseInt; I profiled it and
	 * saw a big performance improvement to the overall load process.)
	 * @param input String
	 * @param pos Position in string of first digit
	 * @return Value as integer
	 */
	static int getTwoDigitHexInt(String input, int pos)
	{
		char a = input.charAt(pos), b = input.charAt(pos+1);
		int high = a > '9' ? (a - 'a' + 10) : (a - '0');
		int low = b > '9' ? (b - 'a' + 10) : (b - '0');
		return high << 4 | low;
	}

	/**
	 * @param kanji Kanji character (should be a single character, but may be
	 *   a UTF-16 surrogate pair)
	 * @param directions Strokes string (in {@link #getAllDirections()} format)
	 * @param full Full summary string (in {@link #getFullSummary()} format)
	 * @throws IllegalArgumentException If strokes string has invalid format
	 */
	KanjiInfo(String kanji, String directions, String full) throws IllegalArgumentException
	{
		this.kanji = kanji;

		int count = (full.length()+1) / 12;
		if(count < 1 || (count * 6 -3) != directions.length())
		{
			throw new IllegalArgumentException("Invalid directions (" + directions 
				+ ") for kanji (" + kanji + ")");
		}
		if((count * 12 - 1) != full.length())
		{
			throw new IllegalArgumentException("Invalid full (" + full 
				+ ") for kanji (" + kanji + ")");
		}
		
		strokeDirections = new Direction[count];
		strokeStarts = new Location[count];
		strokeEnds = new Location[count];
		moveDirections = new Direction[count-1];
		
		try
		{
			int offset = 0;
			for(int i=0; i<count; i++)
			{
				if(i != 0)
				{
					offset++; // Skip colon
					moveDirections[i-1] = Direction.fromString(directions.charAt(offset++) + "");
					offset++; // Skip colon
				}
	
				strokeStarts[i] = Location.fromString(directions.charAt(offset++) + "");
				strokeDirections[i] = Direction.fromString(directions.charAt(offset++) + "");
				strokeEnds[i] = Location.fromString(directions.charAt(offset++) + "");
			}
		}
		catch(IllegalArgumentException e)
		{
			throw new IllegalArgumentException("Invalid strokes(" + directions
				+ ") for kanji (" + kanji + ")");
		}

		try
		{
			strokes = new Stroke[count];
			int offset = 0;
			for(int i=0; i<count; i++)
			{
				if(i != 0)
				{
					offset++; // Skip colon
				}
				
				strokes[i] = new Stroke(
					Integer.parseInt(full.substring(offset, offset+2), 16),				
					Integer.parseInt(full.substring(offset+3, offset+5), 16),				
					Integer.parseInt(full.substring(offset+6, offset+8), 16),				
					Integer.parseInt(full.substring(offset+9, offset+11), 16));
				offset+=11;
			}
		}
		catch(NumberFormatException e)
		{
			e.printStackTrace();
			throw new IllegalArgumentException("Invalid summary(" + full
				+ ") for kanji (" + kanji + ")");
		}
	}

	/**
	 * Adds a stroke. Can only be called during initialisation.
	 * @param stroke New stroke
	 * @throws IllegalStateException If already finished
	 */
	public synchronized void addStroke(InputStroke stroke) throws IllegalStateException	
	{
		if(loadingStrokes == null)
		{
			throw new IllegalStateException("Cannot add strokes after loading");
		}
		loadingStrokes.add(stroke);
	}
	
	/**
	 * Marks kanji as finished, normalising all strokes.
	 * @throws IllegalStateException If already finished
	 */
	public synchronized void finish() throws IllegalStateException	
	{
		if(loadingStrokes == null)
		{
			throw new IllegalStateException("Cannot finish more than once");
		}
		
		// Get stroke array and normalise it
		InputStroke[] inputStrokes = loadingStrokes.toArray(
			new InputStroke[loadingStrokes.size()]);
		strokes = InputStroke.normalise(inputStrokes);
		
		// Find directions
		findDirections();
	}

	/**
	 * Calculate the direction summary.
	 */
	private void findDirections()
	{
		// Find all the directions
		strokeDirections = new Direction[strokes.length];
		strokeStarts = new Location[strokes.length];
		strokeEnds = new Location[strokes.length];
		for(int i=0; i<strokes.length; i++)
		{
			strokeDirections[i] = strokes[i].getDirection();
			strokeStarts[i] = strokes[i].getStartLocation();
			strokeEnds[i] = strokes[i].getEndLocation();
		}
		moveDirections = new Direction[strokes.length == 0 ? 0 : strokes.length - 1];
		for(int i=1; i<strokes.length; i++)
		{
			moveDirections[i-1] = strokes[i].getMoveDirection(strokes[i-1]);
		}
	}
	
	/**
	 * Checks that this kanji has been finished.
	 * @throws IllegalStateException If not finished
	 */
	private void checkFinished() throws IllegalStateException
	{
		if(strokeDirections == null)
		{
			throw new IllegalStateException("Cannot call on unfinished kanji");
		}
	}
	
	/** 
	 * @return Kanji character (one character or a two-character surrogate pair)
	 */
	public String getKanji()
	{
		return kanji;
	}
	
	/**
	 * @return Stroke count
	 * @throws IllegalStateException If not finished
	 */
	public int getStrokeCount() throws IllegalStateException
	{
		checkFinished();
		return strokeDirections.length;
	}
	
	/**
	 * @param index Stroke index
	 * @return Stroke
	 * @throws ArrayIndexOutOfBoundsException If index >= 
	 *   {@link #getStrokeCount()}
	 * @throws IllegalStateException If loaded in a way that doesn't give
	 *   these
	 */
	Stroke getStroke(int index) throws ArrayIndexOutOfBoundsException,
		IllegalStateException
	{
		if(strokes == null)
		{
			throw new IllegalStateException("Cannot call getStroke in this state");
		}
		
		return strokes[index];
	}
	
	/**
	 * Obtains all the directions (stroke and move).
	 * @return All the direction arrows
	 */
	public String getAllDirections()
	{
		StringBuilder out = new StringBuilder();
		for(int i=0; i<strokeDirections.length; i++)
		{
			if(i>0)
			{
				out.append(':');
				out.append(moveDirections[i-1]);
				out.append(':');
			}
			out.append(strokeStarts[i]);
			out.append(strokeDirections[i]);
			out.append(strokeEnds[i]);
		}
		return out.toString();
	}
	
	private String getTwoDigitPosition(int intPos)
	{
		String result = Integer.toHexString(intPos);
		if(result.length() == 1)
		{
			result = "0" + result;
		}
		return result;
	}
	
	/**
	 * Obtains all stroke details as a from/to summary.
	 * @return Full details as string
	 */
	public String getFullSummary()
	{
		if(strokes == null)
		{
			throw new IllegalStateException("Strokes not available");			
		}
		
		StringBuilder out = new StringBuilder();
		for(Stroke stroke : strokes)
		{
			if(out.length() > 0)
			{
				out.append(':');
			}
			out.append(getTwoDigitPosition(stroke.getStartX()));
			out.append(',');
			out.append(getTwoDigitPosition(stroke.getStartY()));
			out.append('-');
			out.append(getTwoDigitPosition(stroke.getEndX()));
			out.append(',');
			out.append(getTwoDigitPosition(stroke.getEndY()));
		}

		return out.toString();
	}
	
	/**
	 * Writes the basic info from this kanji to short XML format data.
	 * @param out Writer that receives data
	 * @throws IOException Any error
	 */
	public void write(Writer out) throws IOException
	{
		out.write("<kanji unicode='"
			+ Integer.toHexString(Character.codePointAt(kanji, 0)).toUpperCase()
			+ "' strokes='" + getFullSummary() + "'/>\n");
	}
	
	private final static float STROKE_DIRECTION_WEIGHT = 1.0f;
	private final static float MOVE_DIRECTION_WEIGHT = 0.8f;
	private final static float STROKE_LOCATION_WEIGHT = 0.6f;
	
	private final static float CLOSE_WEIGHT = 0.7f;
	
	/**
	 * Gets a score for matching with the specified other kanji. Scores are
	 * only comparable against other kanji with same stroke count.
	 * @param other Other kanji
	 * @param algo Match algorithm to use
	 * @return Score
	 * @throws IllegalArgumentException If other kanji has different stroke count
	 */
	public float getMatchScore(KanjiInfo other, MatchAlgorithm algo) 
		throws IllegalArgumentException
	{
		switch(algo)
		{
		case STRICT:
			return getStrictMatchScore(other);
		case FUZZY:
		case FUZZY_1OUT:
		case FUZZY_2OUT:
			if(fuzzyComparer == null)
			{
				fuzzyComparer = new FuzzyComparer(this);
			}
			return fuzzyComparer.getMatchScore(other);
		default:
			throw new IllegalArgumentException("Unknown algorithm");
		}
	}
	
	/**
	 * Gets a score for matching with the specified other kanji. Scores are
	 * only comparable against other kanji with same stroke count.
	 * @param other Other kanji
	 * @return Score
	 * @throws IllegalArgumentException If other kanji has different stroke count
	 */
	private float getStrictMatchScore(KanjiInfo other) throws IllegalArgumentException
	{
		if(other.strokeStarts.length != strokeStarts.length)
		{
			throw new IllegalArgumentException(
				"Can only compare with same match length");
		}
		
		float score = 0;
		for(int i=0; i<strokeStarts.length; i++)
		{
			// Stroke direction
			if(strokeDirections[i] == other.strokeDirections[i])
			{
				score += STROKE_DIRECTION_WEIGHT;
			}
			else if(strokeDirections[i].isClose(other.strokeDirections[i]))
			{
				score += STROKE_DIRECTION_WEIGHT * CLOSE_WEIGHT;
			}
			
			// Move direction
			if(i>0)
			{
				if(moveDirections[i-1] == other.moveDirections[i-1])
				{
					score += MOVE_DIRECTION_WEIGHT;
				}
				else if(moveDirections[i-1].isClose(other.moveDirections[i-1]))
				{
					score += MOVE_DIRECTION_WEIGHT * CLOSE_WEIGHT;
				}
			}
			
			// Start and end locations
			if(strokeStarts[i] == other.strokeStarts[i])
			{
				score += STROKE_LOCATION_WEIGHT;
			}
			else if(strokeStarts[i].isClose(other.strokeStarts[i]))
			{
				score += STROKE_LOCATION_WEIGHT * CLOSE_WEIGHT;
			}
			if(strokeEnds[i] == other.strokeEnds[i])
			{
				score += STROKE_LOCATION_WEIGHT;
			}
			else if(strokeEnds[i].isClose(other.strokeEnds[i]))
			{
				score += STROKE_LOCATION_WEIGHT * CLOSE_WEIGHT;
			}
		}
		
		float max = strokeStarts.length * (STROKE_DIRECTION_WEIGHT + 2 * STROKE_LOCATION_WEIGHT) +
			(strokeStarts.length - 1) * MOVE_DIRECTION_WEIGHT;
		
		return 100.0f * score / max;
	}
}
