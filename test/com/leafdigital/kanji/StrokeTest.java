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

import static org.junit.Assert.*;

import org.junit.Test;

import com.leafdigital.kanji.Stroke;
import com.leafdigital.kanji.Stroke.Location;

/** Test stroke features. */
public class StrokeTest
{
	/**
	 * Tests creating a stroke by supplying parameters.
	 * @throws Exception Any error
	 */
	@Test
	public void testCreateBasic() throws Exception
	{
		Stroke stroke = new Stroke(1, 2, 3, 4);
		assertStroke(stroke, 1, 2, 3, 4);
	}
	
	/**
	 * Tests direction analysis.
	 * @throws Exception Any error
	 */
	@Test
	public void testDirection() throws Exception
	{
		Stroke stroke = new Stroke(0.5f, 0.5f, 0.51f, 0.51f);
		assertEquals(Stroke.Direction.X, stroke.getDirection());
		
		// Exact straights
		stroke = new Stroke(0.5f, 0.5f, 0.8f, 0.5f);
		assertEquals(Stroke.Direction.E, stroke.getDirection());
		stroke = new Stroke(0.5f, 0.5f, 0.5f, 0.8f);
		assertEquals(Stroke.Direction.S, stroke.getDirection());
		stroke = new Stroke(0.5f, 0.5f, 0.2f, 0.5f);
		assertEquals(Stroke.Direction.W, stroke.getDirection());
		stroke = new Stroke(0.5f, 0.5f, 0.5f, 0.2f);
		assertEquals(Stroke.Direction.N, stroke.getDirection());
		
		// Exact diagonals
		stroke = new Stroke(0.5f, 0.5f, 0.8f, 0.8f);
		assertEquals(Stroke.Direction.SE, stroke.getDirection());
		stroke = new Stroke(0.5f, 0.5f, 0.2f, 0.8f);
		assertEquals(Stroke.Direction.SW, stroke.getDirection());
		stroke = new Stroke(0.5f, 0.5f, 0.2f, 0.2f);
		assertEquals(Stroke.Direction.NW, stroke.getDirection());
		stroke = new Stroke(0.5f, 0.5f, 0.8f, 0.2f);
		assertEquals(Stroke.Direction.NE, stroke.getDirection());
		
		// Rough straights
		stroke = new Stroke(0.5f, 0.5f, 0.8f, 0.52f);
		assertEquals(Stroke.Direction.E, stroke.getDirection());
		stroke = new Stroke(0.5f, 0.5f, 0.8f, 0.48f);
		assertEquals(Stroke.Direction.E, stroke.getDirection());
		
		// Rough diagonals
		stroke = new Stroke(0.5f, 0.5f, 0.8f, 0.6f);
		assertEquals(Stroke.Direction.SE, stroke.getDirection());
		stroke = new Stroke(0.5f, 0.5f, 0.8f, 0.4f);
		assertEquals(Stroke.Direction.NE, stroke.getDirection());
	}
	
	/**
	 * Tests move direction.
	 * @throws Exception Any error
	 */
	@Test
	public void testMoveDirection() throws Exception
	{
		InputStroke[] inputStrokes =
		{
			new InputStroke("M23.78,21.29" 
				+ "c3.6,0.9,6.76,0.85,10.36,0.3" 
				+ "c10.48-1.6,38.27-5.5,40.43-5.84" 
				+ "c3.93-0.62,4.68,1.86,2.07,4.08" 
				+ "c-2.6,2.22-14.89,12.42-21.68,17.44"),
			new InputStroke("M51.94,38.24" 
				+ "C61.5,42.5,64.75,70.25,57.89,90" 
				+ "c-3.24,9.32-8.64,2.5-10.39,0.5")
		};
		Stroke[] strokes = InputStroke.normalise(inputStrokes);
		assertEquals(Stroke.Direction.X, strokes[1].getMoveDirection(strokes[0]));
	}
	
	/**
	 * Tests locations.
	 * @throws Exception Any error
	 */
	@Test
	public void testLocation() throws Exception
	{
		Stroke stroke = new Stroke(0.1f, 0.1f, 0.4f, 0.1f);
		assertEquals(Location.NW, stroke.getStartLocation());
		assertEquals(Location.N, stroke.getEndLocation());

		stroke = new Stroke(0.7f, 0.1f, 0.9f, 0.4f);
		assertEquals(Location.NE, stroke.getStartLocation());
		assertEquals(Location.E, stroke.getEndLocation());
		
		stroke = new Stroke(0.8f, 0.94f, 0.4f, 0.7f);
		assertEquals(Location.SE, stroke.getStartLocation());
		assertEquals(Location.S, stroke.getEndLocation());
		
		stroke = new Stroke(0.2f, 0.9f, 0.3f, 0.5f);
		assertEquals(Location.SW, stroke.getStartLocation());
		assertEquals(Location.W, stroke.getEndLocation());

		stroke = new Stroke(0.4f, 0.4f, 0.6f, 0.6f);
		assertEquals(Location.MID, stroke.getStartLocation());
		assertEquals(Location.MID, stroke.getEndLocation());
	}
	
	/**
	 * Tests direction comparison.
	 * @throws Exception Any error
	 */
	@Test
	public void testDirectionCompare() throws Exception
	{
		// One edge
		assertFalse(Stroke.Direction.N.isClose(Stroke.Direction.E));
		assertTrue(Stroke.Direction.N.isClose(Stroke.Direction.NE));
		assertTrue(Stroke.Direction.N.isClose(Stroke.Direction.N));
		assertTrue(Stroke.Direction.N.isClose(Stroke.Direction.NW));
		assertFalse(Stroke.Direction.N.isClose(Stroke.Direction.W));
		
		// Middle
		assertFalse(Stroke.Direction.S.isClose(Stroke.Direction.E));
		assertTrue(Stroke.Direction.S.isClose(Stroke.Direction.SE));
		assertTrue(Stroke.Direction.S.isClose(Stroke.Direction.S));
		assertTrue(Stroke.Direction.S.isClose(Stroke.Direction.SW));
		assertFalse(Stroke.Direction.S.isClose(Stroke.Direction.W));
		
		// compare with X
		assertTrue(Stroke.Direction.W.isClose(Stroke.Direction.X));
		assertTrue(Stroke.Direction.X.isClose(Stroke.Direction.W));
		assertTrue(Stroke.Direction.X.isClose(Stroke.Direction.X));
	}

	static void assertStroke(Stroke stroke,
		int startX, int startY, int endX, int endY)
	{
		assertEquals("Incorrect start X " + stroke, startX, stroke.getStartX());
		assertEquals("Incorrect start Y " + stroke, startY, stroke.getStartY());
		assertEquals("Incorrect end X " + stroke, endX, stroke.getEndX());
		assertEquals("Incorrect end Y " + stroke, endY, stroke.getEndY());
	}
}
