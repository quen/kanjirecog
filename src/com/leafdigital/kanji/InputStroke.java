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
 * Single kanji stroke as represented in floating-point numbers.
 * <p>
 * This class has two main functions: first, to represent strokes loaded from 
 * the KanjiVG file as SVG paths, and second, to represent strokes being drawn 
 * by user input. These need to be stored as float values so that they can be 
 * scaled later when all strokes are available.
 */
public class InputStroke
{
	private float startX, startY, endX, endY;
	
	/**
	 * Class to make it easier to read path data.
	 */
	private class PathData
	{
		private final static int EOL = -1, NUMBER = -2;

		String remaining;
		private PathData(String path)
		{
			this.remaining = path;
		}
		
		/**
		 * Reads the next non-whitespace character.
		 * @return Character or EOL if end of string or NUMBER if number/comma not letter
		 */
		private int readLetter()
		{
			int pos = 0;
			while(true)
			{
				if(pos == remaining.length())
				{
					return EOL;
				}
				char letter = remaining.charAt(pos);
				if (!Character.isWhitespace(letter))
				{
					if(letter == ',' || letter == '-' || letter == '+' || (letter >= '0' && letter <= '9'))
					{
						return NUMBER;
					}
					remaining = remaining.substring(pos+1);
					return letter;
				}
				pos++;
			}
		}
		
		/**
		 * Reads the next number, skipping whitespace and comma and +
		 * @return Number
		 * @throws IllegalArgumentException If unexpected EOL or invalid number
		 */
		private float readNumber()
		{
			int start = 0;
			while(true)
			{
				if(start == remaining.length())
				{
					throw new IllegalArgumentException("Unexpected EOL before number");
				}
				char c = remaining.charAt(start);
				if(c != ',' && !Character.isWhitespace(c) && c != '+')
				{
					break;
				}
				start++;
			}
			
			int end = start + 1;
			while(true)
			{
				if(end == remaining.length())
				{
					break;
				}
				char c = remaining.charAt(end);
				if(c != '.' && (c < '0' || c > '9'))
				{
					break;
				}
				end++;
			}
			
			String number = remaining.substring(start, end);
			remaining = remaining.substring(end);
			
			try
			{
				return Float.parseFloat(number);
			}
			catch(NumberFormatException e)
			{
				throw new IllegalArgumentException("Invalid number: " + number);
			}
		}
		
	}
	
	/**
	 * Constructs from an SVG path. The full SVG path sequence is not accepted.
	 * Instead all paths must begin with a single M or m followed by commands from
	 * the following list: "CcSsZz"
	 * @param svgPath SVG path string
	 * @throws IllegalArgumentException If string can't be parsed
	 */
	public InputStroke(String svgPath) throws IllegalArgumentException
	{
		PathData data = new PathData(svgPath);

		// Read initial M
		int initial = data.readLetter();
		if(initial != 'M' && initial != 'm')
		{
			throw new IllegalArgumentException("Path must start with M");
		}
		
		// Read start co-ordinates (note: 'm' is not really relative at start
		// of path, so treated the same as M; see SVG spec)
		startX = data.readNumber();
		startY = data.readNumber();
		
		// Handle all other commands
		float x = startX, y = startY;
		int lastCommand = -1;
		loop: while(true)
		{
			int command = data.readLetter();
			if(command == PathData.NUMBER)
			{
				if(lastCommand == -1)
				{
					throw new IllegalArgumentException("Expecting command, not number");
				}
				command = lastCommand;
			}
			else
			{
				lastCommand = command;
			}
			switch(command)
			{
			case PathData.EOL : 
				break loop; // End of line
			case 'c' :
				data.readNumber();
				data.readNumber();
				data.readNumber();
				data.readNumber();
				x += data.readNumber();
				y += data.readNumber();
				break;
			case 'C' : 
				data.readNumber();
				data.readNumber();
				data.readNumber();
				data.readNumber();
				x = data.readNumber();
				y = data.readNumber();
				break;
			case 's' :
				data.readNumber();
				data.readNumber();
				x += data.readNumber();
				y += data.readNumber();
				break;
			case 'S' :
				data.readNumber();
				data.readNumber();
				x = data.readNumber();
				y = data.readNumber();
				break;
			case 'z' :
			case 'Z' :
				x = startX;
				y = startY;
				break;
			default :
				throw new IllegalArgumentException("Unexpected path command: " 
					+	(char)command);
			}
		}
		
		endX = x;
		endY = y;
	}
	
	/**
	 * Constructs from raw data.
	 * @param startX Start position (x) 0-1
	 * @param startY Start position (y) 0-1
	 * @param endX End position (x) 0-1
	 * @param endY End position (y) 0-1
	 */
	public InputStroke(float startX, float startY, float endX, float endY)
	{
		this.startX = startX;
		this.endX = endX;
		this.startY = startY;
		this.endY = endY;
	}
	
	/** 
	 * @return Start X position
	 */
	public float getStartX()
	{
		return startX;
	}
	
	/** 
	 * @return End X position 
	 */
	public float getEndX()
	{
		return endX;
	}
	
	/** 
	 * @return Start Y position
	 */
	public float getStartY()
	{
		return startY;
	}
	
	/** 
	 * @return End Y position
	 */
	public float getEndY()
	{
		return endY;
	}
	
	/**
	 * Normalises an array of strokes by converting their co-ordinates to range
	 * from 0 to 1 in each direction. If the stroke bounding rectangle
	 * has width or height 0, this will be handled so that it is at 0.5 in
	 * the relevant position.
	 * <p>
	 * This works by constructing new stroke objects; strokes are final.
	 * @param strokes Stroke array to convert
	 * @return Resulting converted array
	 */
	public static Stroke[] normalise(InputStroke[] strokes)
	{
		// Find range
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, 
			maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
		for(InputStroke stroke : strokes)
		{
			if(stroke.startX < minX)
			{
				minX = stroke.startX;
			}
			if(stroke.startX > maxX)
			{
				maxX = stroke.startX;
			}
			if(stroke.startY < minY)
			{
				minY = stroke.startY;
			}
			if(stroke.startY > maxY)
			{
				maxY = stroke.startY;
			}
			
			if(stroke.endX < minX)
			{
				minX = stroke.endX;
			}
			if(stroke.endX > maxX)
			{
				maxX = stroke.endX;
			}
			if(stroke.endY < minY)
			{
				minY = stroke.endY;
			}
			if(stroke.endY > maxY)
			{
				maxY = stroke.endY;
			}
		}
		
		// Adjust max/min to avoid divide by zero
		if(abs(minX - maxX) < 0.0000000001f)
		{
			// Adjust by 1% of height
			float adjust = abs(minY - maxY) / 100f;
			if(adjust < 0.0000000001f)
			{
				adjust = 0.1f;
			}
			minX -= adjust;
			maxX += adjust;
		}
		if(abs(minY - maxY) < 0.0000000001f)
		{
			// Adjust by 1% of width
			float adjust = abs(minX - maxX) / 100f;
			if(adjust < 0.0000000001f)
			{
				adjust = 0.1f;
			}
			
			minY -= adjust;
			maxY += adjust;
		}
		
		// Now sort out a maximum scale factor, so that very long/thin kanji
		// don't get stretched to square
		float xRange = abs(minX - maxX), yRange = abs(minY - maxY);
		if(xRange > 5f * yRange)
		{
			float adjust = (xRange - yRange) / 2;
			minY -= adjust;
			maxY += adjust;
		}
		else if(yRange > 5f * xRange)
		{
			float adjust = (yRange - xRange) / 2;
			minX -= adjust;
			maxX += adjust;
		}
		
		// Convert all points according to range
		Stroke[] output = new Stroke[strokes.length];
		for(int i=0; i<strokes.length; i++)
		{
			output[i] = new Stroke(
				(strokes[i].startX - minX) / (maxX - minX),
				(strokes[i].startY - minY) / (maxY - minY),
				(strokes[i].endX - minX) / (maxX - minX),
				(strokes[i].endY - minY) / (maxY - minY));
		}
		
		return output;
	}
	
	private static float abs(float value)	
	{	
		return value < 0 ? -value : value;
	}
	
	@Override
	public String toString()
	{
		return "[" + startX + "," + startY + ":" + endX + "," + endY + "]";
	}
}
