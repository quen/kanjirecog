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
 * Single kanji stroke.
 */
public class Stroke
{
	// All values in range 0-255
	private int startX, startY, endX, endY;
	
	/**
	 * Constructs from float data.
	 * @param startX Start position (x) 0-1
	 * @param startY Start position (y) 0-1
	 * @param endX End position (x) 0-1
	 * @param endY End position (y) 0-1
	 * @throws IllegalArgumentException If any value out of range
	 */
	Stroke(float startX, float startY, float endX, float endY)
		throws IllegalArgumentException
	{
		this(convert(startX), convert(startY), convert(endX), convert(endY));
	}
	
	private static int convert(float value)
	{
		return (int)(value * 255 + 0.49999);
	}

	/**
	 * Constructs from the raw data.
	 * @param startX Start position (x) 0-255
	 * @param startY Start position (y) 0-255
	 * @param endX End position (x) 0-255
	 * @param endY End position (y) 0-255
	 * @throws IllegalArgumentException If any value out of range
	 */
	Stroke(int startX, int startY, int endX, int endY)
		throws IllegalArgumentException
	{
		if(startX < 0 || startX > 255 || startY < 0 || startY > 255 
			|| endX < 0 || endX > 255 || endY < 0 || endY > 255) {
			throw new IllegalArgumentException("Value out of range");
		}
		this.startX = startX;
		this.endX = endX;
		this.startY = startY;
		this.endY = endY;
	}
	
	/** 
	 * @return Start X position
	 */
	public int getStartX()
	{
		return startX;
	}
	
	/** 
	 * @return End X position 
	 */
	public int getEndX()
	{
		return endX;
	}
	
	/** 
	 * @return Start Y position
	 */
	public int getStartY()
	{
		return startY;
	}
	
	/** 
	 * @return End Y position
	 */
	public int getEndY()
	{
		return endY;
	}
	
	/**
	 * Represents approximate location of start/end points of stroke.
	 */
	public enum Location
	{
		/** Basically N */
		N(1, 0, "\u2580"), 
		/** Basically NE */
		NE(2, 0, "\u259c"), 
		/** Basically E */
		E(2, 1, "\u2590"), 
		/** Basically SE */
		SE(2, 2, "\u259f"),
		/** Basically S */
		S(1, 2, "\u2584"), 
		/** Basically SW */
		SW(0, 2, "\u2599"), 
		/** Basically W */
		W(0, 1, "\u258c"),
		/** Basically NW */
		NW(0, 0, "\u259b"),
		/** Basically in the middle */
		MID(1, 1, "\u2588");

		private int x, y;
		private String display;
		
		Location(int x, int y, String display)
		{
			this.x = x;
			this.y = y;
			this.display = display;
		}
		
		@Override
		public String toString()
		{
			return display;
		}
		
		/**
		 * Reads from string.
		 * @param s Input string
		 * @return Direction value
		 * @throws IllegalArgumentException If not a valid direction
		 */
		public static Location fromString(String s) throws IllegalArgumentException
		{
			for(Location location : Location.values())
			{
				if(location.display.equals(s))
				{
					return location;
				}
			}
			throw new IllegalArgumentException("Unknown location (" + s + ")");
		}
		
		/**
		 * @param other Another direction
		 * @return True if this direction is within one step of the other direction
		 */
		public boolean isClose(Location other)
		{
			return Math.abs(x - other.x) <= 1 && Math.abs(y - other.y) <= 1;
		}
		
		/**
		 * @param x Normalised X
		 * @param y Normalised Y
		 * @return Location
		 */
		public static Location get(float x, float y)
		{
			if(x < 85)
			{
				if(y < 85)
				{
					return Location.NW;
				}
				else if(y < 170)
				{
					return Location.W;
				}
				else
				{
					return Location.SW;
				}
			}
			else if(x < 170)
			{
				if(y < 85)
				{
					return Location.N;
				}
				else if(y < 170)
				{
					return Location.MID;
				}
				else
				{
					return Location.S;
				}
			}
			else
			{
				if(y < 85)
				{
					return Location.NE;
				}
				else if(y < 170)
				{
					return Location.E;
				}
				else
				{
					return Location.SE;
				}
			}
		}
	}
	
	/**
	 * The direction of a stroke.
	 */
	public enum Direction
	{
		/** Basically N */
		N(0, "\u2191"), 
		/** Basically NE */
		NE(1, "\u2197"), 
		/** Basically E */
		E(2, "\u2192"), 
		/** Basically SE */
		SE(3, "\u2198"),
		/** Basically S */
		S(4, "\u2193"), 
		/** Basically SW */
		SW(5, "\u2199"), 
		/** Basically W */
		W(6, "\u2190"),
		/** Basically NW */
		NW(7, "\u2196"),
		/** No clear movement */
		X(-1, "\u26aa");
		
		private int index;
		private String display;
		
		Direction(int index, String display)
		{
			this.index = index;
			this.display = display;
		}
		
		@Override
		public String toString()
		{
			return display;
		}
		
		/**
		 * Reads from string.
		 * @param s Input string
		 * @return Direction value
		 * @throws IllegalArgumentException If not a valid direction
		 */
		public static Direction fromString(String s) throws IllegalArgumentException
		{
			for(Direction direction : Direction.values())
			{
				if(direction.display.equals(s))
				{
					return direction;
				}
			}
			throw new IllegalArgumentException("Unknown direction (" + s + ")");
		}
		
		/**
		 * @param other Another direction
		 * @return True if this direction is within one step of the other direction
		 */
		public boolean isClose(Direction other)
		{
			if(this==X || other==X || this == other)
			{
				return true;
			}
			return (this.index == ( (other.index + 1) % 8 ) ) 
				|| ( ((this.index + 1) % 8 ) == other.index);
		}
		
		/**
		 * Threshold above which something counts as directional.
		 */
		private static int DIRECTION_THRESHOLD = 51;

		/**
		 * Propotion (out of 256) of dominant movement required to count as diagonal.
		 * (E.g. if this is 77 = approx 30%, and if movement S is 10, then movenent E must
		 * be at least 10 * 77 / 256 in order to count as SE).
		 */
		private static int DIAGONAL_THRESHOLD = 77;
		
		/**
		 * Calculates the direction between two points.
		 * @param startX Start X
		 * @param startY Start Y
		 * @param endX End X
		 * @param endY End Y
		 * @return Direction of stroke
		 * @throws IllegalStateException If not normalised
		 */
		private static Direction get(int startX, int startY,
			int endX, int endY) throws IllegalStateException
		{
			// Get movement in each direction
			int deltaX = endX - startX, deltaY = endY - startY;
			
			// Check if it's not really movement at all (under threshold)
			int absDeltaX = Math.abs(deltaX), absDeltaY = Math.abs(deltaY);
			if(absDeltaX < DIRECTION_THRESHOLD && absDeltaY < DIRECTION_THRESHOLD)
			{
				return Direction.X;
			}
			
			if(absDeltaX > absDeltaY)
			{
				// X movement is more significant
				boolean diagonal = absDeltaY > ((DIAGONAL_THRESHOLD * absDeltaX) >> 8);
				if(deltaX > 0)
				{
					if(diagonal)
					{
						return deltaY < 0 ? Direction.NE : Direction.SE;
					}
					else
					{
						return Direction.E;
					}
				}
				else
				{
					if(diagonal)
					{
						return deltaY < 0 ? Direction.NW : Direction.SW;
					}
					else
					{
						return Direction.W;
					}
				}
			}
			else
			{
				// Y movement is more significant
				boolean diagonal = absDeltaX > ((DIAGONAL_THRESHOLD * absDeltaY) >> 8);
				if(deltaY > 0)
				{
					if(diagonal)
					{
						return deltaX < 0 ? Direction.SW : Direction.SE;
					}
					else
					{
						return Direction.S;
					}
				}
				else
				{
					if(diagonal)
					{
						return deltaX < 0 ? Direction.NW : Direction.NE;
					}
					else
					{
						return Direction.N;
					}
				}
			}
		}		
	}
	
	/**
	 * Calculates the direction of this stroke.
	 * @return Direction of stroke
	 */
	public Direction getDirection()
	{
		return Direction.get(startX, startY, endX, endY);
	}
	
	/**
	 * Calculates the direction that the pen moved between the end of the
	 * last stroke and the start of this one.
	 * @param previous Previous stroke
	 * @return Direction moved
	 */
	public Direction getMoveDirection(Stroke previous)
	{
		return Direction.get(previous.endX, previous.endY, startX, startY);
	}
	
	/**
	 * @return Approximate location of start of stroke
	 */
	public Location getStartLocation() 
	{
		return Location.get(startX, startY);
	}
	
	/**
	 * @return Approximate location of end of stroke
	 */
	public Location getEndLocation()
	{
		return Location.get(endX, endY);
	}
	
	@Override
	public String toString()
	{
		return "[" + startX + "," + startY + ":" + endX + "," + endY + "]";
	}
}
