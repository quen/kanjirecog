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

/** Test stroke features. */
public class InputStrokeTest
{
	/**
	 * Tests creating a stroke from a path.
	 * @throws Exception Any error
	 */
	@Test
	public void testCreatePath() throws Exception
	{
		// M and c
		InputStroke stroke = new InputStroke(
			"M8.75,23.62c1.5,1.25,2.16,3.14,2.38,5.38c0.96,10.07,0.89,8.17,1.89,19.38" 
			+ "c0.31,3.4,0.59,6.58,0.86,9.38");
		assertInputStroke(stroke, 8.75, 23.62, 
			8.75 + 2.38 + 1.89 + 0.86, 23.62 + 5.38 + 19.38 + 9.38);

		// m and c (with c used multiple times without repeating command)
		stroke = new InputStroke(
			"m7.4963375,37.394827" 
			+ "c1.1989328,0.99911,1.7264632,2.509766,1.9023067,4.300172," 
			+	"0.7673168,8.048836,0.7113668,6.530187,1.5106558,15.490212," 
			+ "0.247779,2.717581,0.47158,5.259319,0.687388,7.497327");
		assertInputStroke(stroke, 7.4963375, 37.394827,
			7.4963375 + 1.9023067 + 1.5106558 + 0.687388,
			37.394827 + 4.300172 + 15.490212 + 7.497327);
		
		// M and c and s
		stroke = new InputStroke(
			"M19.25,16.75c0.75,1.25,1.25,3.25,1,5.5s0.25,68.5,0.25,72.25");
		assertInputStroke(stroke, 19.25, 16.75, 19.25 + 1 + 0.25, 16.75 + 5.5 + 72.25);	
		
		// M and c and C
		stroke = new InputStroke(
			"M71.81,39.33c0.12,0.85,0.22,2.03-0.62,3.03" 
			+ "C60.33,55.27,50.25,66.5,30.25,76.79");
		assertInputStroke(stroke, 71.81, 39.33, 30.25, 76.79);
		
		// M and c and S
		stroke = new InputStroke(
			"M17.33,15.25c0.59,0.41,1.13,2.19,1.33,2.74" 
			+	"c0.2,0.55-0.3,15.37-0.3,17.09c0,6.42,5.68,6.74,15.16,6.74" 
			+	"S49,41.5,49,34.43");
		assertInputStroke(stroke, 17.33, 15.25, 49, 34.43);
		
		// M and c and s and C and z
		stroke = new InputStroke(
			"M6.93,103.36c3.61-2.46,6.65-6.21,6.65-13.29c0-1.68-1.36-3.03-3.03-3.03" 
			+	"s-3.03,1.36-3.03,3.03s1.36,3.03,3.03,3.03" 
			+	"C15.17,93.1,10.4,100.18,6.93,103.36z");
		assertInputStroke(stroke, 6.93, 103.36, 6.93, 103.36);		
	}
	
	/**
	 * Tests creating a stroke by supplying parameters.
	 * @throws Exception Any error
	 */
	@Test
	public void testCreateBasic() throws Exception
	{
		InputStroke stroke = new InputStroke(1, 2, 3, 4);
		assertInputStroke(stroke, 1, 2, 3, 4);
	}
	
	/**
	 * Tests normalise.
	 * @throws Exception Any error
	 */
	@Test
	public void testNormalise() throws Exception
	{
		// Horizontal line
		InputStroke[] strokes = {
			new InputStroke(7, 4, 77, 4),
		};
		Stroke[] out = InputStroke.normalise(strokes);
		StrokeTest.assertStroke(out[0], 0, 127, 255, 127);
		
		// Vertical line
		strokes = new InputStroke[] {
			new InputStroke(0, 4, 0, -174),
		};
		out = InputStroke.normalise(strokes);
		StrokeTest.assertStroke(out[0], 127, 255, 127, 0);
		
		// Single dot
		strokes = new InputStroke[] {
			new InputStroke(4, 16, 4, 16),
		};
		out = InputStroke.normalise(strokes);
		StrokeTest.assertStroke(out[0], 127, 127, 127, 127);
		
		// Multiple lines
		strokes = new InputStroke[] {
			new InputStroke(100, 40, 150, 10),
			new InputStroke(150, 30, 200, 0),
			new InputStroke(125, 20, 150, 30)
		};
		out = InputStroke.normalise(strokes);
		StrokeTest.assertStroke(out[0], 0, 255, 127, 64);
		StrokeTest.assertStroke(out[1], 127, 191, 255, 0);
		StrokeTest.assertStroke(out[2], 64, 127, 127, 191);
		
		// Very steep diagonal
		strokes = new InputStroke[] {
			new InputStroke(0, 0, 1, 50),
		};
		out = InputStroke.normalise(strokes);
		assertEquals(0, out[0].getStartY());
		assertEquals(255, out[0].getEndY());
		assertTrue(Math.abs(128 - out[0].getStartX()) < 10);
		assertTrue(Math.abs(128 - out[0].getEndX()) < 10);
	}
	
	private static void assertInputStroke(InputStroke stroke,
		double startX, double startY, double endX, double endY)
	{
		assertTrue("Incorrect start X " + stroke, 
			Math.abs(stroke.getStartX() - startX) < 0.001);
		assertTrue("Incorrect end X " + stroke, 
			Math.abs(stroke.getEndX() - endX) < 0.001);
		assertTrue("Incorrect start Y " + stroke, 
			Math.abs(stroke.getStartY() - startY) < 0.001);
		assertTrue("Incorrect end Y " + stroke, 
			Math.abs(stroke.getEndY() - endY) < 0.001);
	}
}
