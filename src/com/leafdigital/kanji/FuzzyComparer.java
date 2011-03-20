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
 * Compares entered strokes with other kanji using slightly fuzzy logic.
 */
public class FuzzyComparer
{
	private Pair[] drawnPairs;
	private Point[] drawnPoints;
	
	private final static float SCOREMULTI_NOT_PAIR = 0.9f;
	private final static float SCOREMULTI_WRONG_DIRECTION = 0.97f;
	private final static float SCOREMULTI_WRONG_COUNT = 0.98f;

	private final static int BEST_SCORES_SORT_FIRST = 5;

	private static class Pair
	{
		private Point a, b;
		
		private int pointCount;
		
		private float[][] scores;
		private float maxBScore, maxAScore;
		
		private float bestScore;
		private int bestAIndex, bestBIndex;
		
		private Pair(Point a, Point b)
		{
			this.a = a;
			this.b = b;
		}
		
		void initDrawn(int maxStrokes)
		{
			scores = new float[maxStrokes * 2][];
			for(int i=0; i<scores.length; i++)
			{
				scores[i] = new float[maxStrokes * 2];
			}
			a.initDrawn(maxStrokes);
			b.initDrawn(maxStrokes);
		}
		
		private void score(Point[] availablePoints)
		{
			pointCount = availablePoints.length;
			maxBScore = -1;
			maxAScore = -1;
			
			// Get max B score
			for(int bIndex=0; bIndex < pointCount; bIndex++)
			{
				int bScore = b.score[bIndex];
				if(bScore > maxBScore)
				{
					maxBScore = bScore;
				}
			}
			
			for(int aIndex=0; aIndex < pointCount; aIndex++)
			{
				// Track max A score
				int aScore = a.score[aIndex];
				if(aScore > maxAScore)
				{
					maxAScore = aScore;
				}
				Pair aPair = availablePoints[aIndex].pair;
				boolean wrongDirection = aPair.a != availablePoints[aIndex];
				
				for(int bIndex=0; bIndex < pointCount; bIndex++)
				{
					int bScore = b.score[bIndex];
					
					if(bIndex==aIndex)
					{
						continue;
					}
					
					// Basic score is sum of individual scores
					float score = aScore + bScore;

					if(aPair != availablePoints[bIndex].pair)
					{
						score *= SCOREMULTI_NOT_PAIR;
					}
					else if(wrongDirection)
					{
						score *= SCOREMULTI_WRONG_DIRECTION;
					}					
					
					scores[aIndex][bIndex] = score;
				}
			}
			
			bestScore = -1f;
		}
		
		private void scoreAvailable(Point[] otherPoints, float mustBeOver)
		{
			// If it hasn't changed since last time, do nothing
			if(bestScore > 0)
			{
				return;
			}
			// If we can't possibly achieve a better score than the current best,
			// return
			if(maxAScore + maxBScore < mustBeOver)
			{
				return;
			}
			
			// Consider all combinations of point A and B
			bestScore = -1f;
//			int loopCount = 0;
			for(int aIndex=0; aIndex < pointCount; aIndex++)
			{
				ScoreAndIndex aScore = a.sortedScore[aIndex];
				int aPointIndex = aScore.index;
				if(aScore.score + maxBScore < mustBeOver 
					|| otherPoints[aPointIndex] == null)
				{
					// If A score + any B score can't beat min score, then continue, or
					// also if point is done
					continue;
				}
				
				float[] correspondingScores = scores[aPointIndex];
				for(int bIndex=0; bIndex < pointCount; bIndex++)
				{
					ScoreAndIndex bScore = b.sortedScore[bIndex];
					int bPointIndex = bScore.index;
					if(bPointIndex == aPointIndex || otherPoints[bPointIndex]==null)
					{
						continue;
					}
					
//					loopCount++;
					
					// Basic score is sum of individual scores
					float score = correspondingScores[bPointIndex];
					
					// Is this best?
					if(score > bestScore)
					{
						bestScore = score;
						bestAIndex = aPointIndex;
						bestBIndex = bPointIndex;
						
						if(bestScore > mustBeOver)
						{
							mustBeOver = bestScore;
						}
					}
				}
			}
			
//			System.err.println(loopCount + "/" + (pointCount * pointCount));
			
		}
	}
	
	private static class ScoreAndIndex implements Comparable<ScoreAndIndex>
	{
		int score, index;
		boolean used;

		@Override
		public int compareTo(ScoreAndIndex o)
		{
			if(o.score > this.score)
			{
				return 1;
			}
			else if(o.score < this.score)
			{
				return -1;
			}
			else
			{
				return index - o.index;
			}
		}
		
		@Override
		public String toString()
		{
			return Integer.toString(score);
		}
	}

	private static class Point
	{
		private final static int SIMILAR_RANGE = 13;
		
		private int x, y;
		private int xLess, xMore, xSimilar, yLess, yMore, ySimilar;
		
		private Pair pair;
		
		private int[] score;
		private ScoreAndIndex[] sortedScore, preSortedScore;
		private int[] best = new int[BEST_SCORES_SORT_FIRST];
		
		private Point(int x, int y)
		{
			this.x = (int) ((x + 0.5f) * 255);
			this.y = (int) ((y + 0.5f) * 255);
		}
		
		private void setPair(Pair pair)
		{
			this.pair = pair;
		}
		
		private void count(Point[] allPoints)
		{
			for(Point point : allPoints)
			{
				if(point != this)
				{
					if(point.x < x - SIMILAR_RANGE)
					{
						xLess++;
					}
					else if(point.x > x + SIMILAR_RANGE)
					{
						xMore++;
					}
					else
					{
						xSimilar++;
					}

					if(point.y < y - SIMILAR_RANGE)
					{
						yLess++;
					}
					else if(point.y > y + SIMILAR_RANGE)
					{
						yMore++;
					}
					else
					{
						ySimilar++;
					}
				}
			}
		}
		
		private void initDrawn(int maxStrokes)
		{
			// Initialise the array only once per drawn character
			score = new int[maxStrokes * 2];
			sortedScore = new ScoreAndIndex[maxStrokes * 2];
			preSortedScore = new ScoreAndIndex[maxStrokes * 2 + 1];
			for(int i=0; i<maxStrokes * 2; i++)
			{
				preSortedScore[i] = new ScoreAndIndex();
			}
			// Dummy score to use for 'best' marker
			preSortedScore[maxStrokes * 2] = new ScoreAndIndex();
		}
		
		
		private void score(Point[] otherPoints, int maxScore)
		{
			for(int i=0; i<BEST_SCORES_SORT_FIRST; i++)
			{
				best[i] = preSortedScore.length-1;
			}
			int worstBestScore = 0;
			for(int i=0; i<otherPoints.length; i++)
			{
				Point other = otherPoints[i];
				
				// Work out difference between each element of these points
				int difference = Math.abs(xLess - other.xLess) 
					+ Math.abs(xMore - other.xMore) + Math.abs(xSimilar - other.xSimilar)
					+ Math.abs(yLess - other.yLess) + Math.abs(yMore - other.yMore)
					+ Math.abs(ySimilar - other.ySimilar);
				
				int thisScore = maxScore - difference;
				preSortedScore[i].index = i;
				preSortedScore[i].score = thisScore;
				preSortedScore[i].used = false;
				score[i] = thisScore;
				
				if(thisScore >= worstBestScore)
				{
					int bestIndex=0;
					for(; bestIndex<BEST_SCORES_SORT_FIRST-1; bestIndex++)
					{
						if(thisScore > preSortedScore[best[bestIndex]].score)
						{
							break;
						}
					}
					for(int moveIndex=BEST_SCORES_SORT_FIRST-1; moveIndex>bestIndex; moveIndex--)
					{
						best[moveIndex] = best[moveIndex-1];
					}
					best[bestIndex] = i;
					if(bestIndex == BEST_SCORES_SORT_FIRST-1)
					{
						worstBestScore = thisScore;
					}
				}
			}
			
			for(int i=0; i<BEST_SCORES_SORT_FIRST; i++)
			{
				sortedScore[i] = preSortedScore[best[i]];
				preSortedScore[best[i]].used = true;
			}
			
			int index = BEST_SCORES_SORT_FIRST;
			for(int i=0; i<otherPoints.length; i++)
			{
				if(!preSortedScore[i].used)
				{
					sortedScore[index++] = preSortedScore[i];
				}
			}
//			System.err.println(Arrays.toString(sortedScore));
			
//			for(int i=otherPoints.length; i<sortedScore.length; i++)
//			{
//				sortedScore[i] = new ScoreAndIndex(0, i);
//			}
//
//			// Sort score into order
//			Arrays.sort(sortedScore);
		}
	}
	
	/**
	 * Construct comparer for a particular drawn kanji.
	 * @param drawn Drawn kanji
	 */
	public FuzzyComparer(KanjiInfo drawn)
	{
		// Set up data about drawn pairs/points
		drawnPairs = convertKanjiInfo(drawn);
		drawnPoints = getPairPoints(drawnPairs);
		for(Pair pair : drawnPairs)
		{
			pair.initDrawn(drawnPairs.length + 2);
		}
	}
	
	private static Pair[] convertKanjiInfo(KanjiInfo info)
	{
		Pair[] result = new Pair[info.getStrokeCount()];
		for(int i=0; i<result.length; i++)
		{
			Stroke stroke = info.getStroke(i);
			result[i] = new Pair(
				new Point(stroke.getStartX(), stroke.getStartY()),
				new Point(stroke.getEndX(), stroke.getEndY()));
		}
		for(Pair pair : result)
		{
			pair.a.setPair(pair);
			pair.b.setPair(pair);
		}
		return result;
	}
	
	private static Point[] getPairPoints(Pair[] pairs)
	{
		Point[] result = new Point[pairs.length * 2];
		int out = 0;
		for(int i=0; i<pairs.length; i++)
		{
			result[out++] = pairs[i].a;
			result[out++] = pairs[i].b;
		}
		for(Point point : result)
		{
			point.count(result);
		}
		return result;
	}
	
	/**
	 * Compares against the given other kanji.
	 * @param other Other kanji
	 * @return Score in range 0 to 100
	 */
	public float getMatchScore(KanjiInfo other)
	{
		// Get data from match kanji
		Pair[] otherPairs = convertKanjiInfo(other);
		Point[] otherPoints = getPairPoints(otherPairs);
		
		// Max difference is (less than) the highest number of strokes *
		// 6 facets.
		int maxScore = Math.max(drawnPoints.length, otherPoints.length) * 6;
		
		// Score all points against all points; O(points^2)
		for(Point point : drawnPoints)
		{
			point.score(otherPoints, maxScore);
		}
		
		// Score all pairs 
		for(Pair pair : drawnPairs)
		{
			pair.score(otherPoints);
		}
		
		// Copy source pairs into list of remaining ones
		Pair[] remainingPairs = new Pair[drawnPairs.length];
		System.arraycopy(drawnPairs, 0, remainingPairs, 0, remainingPairs.length);
		
		// How many remaining things to match?
		int pairsLeft = remainingPairs.length;
		int pointsLeft = otherPoints.length;
		float totalScore = 0f;
		
		while(pointsLeft > 0 && pairsLeft > 0)
		{
			// Score all pairs to find best match
			int bestPairIndex = -1;
			Pair bestPair = null;
			float bestPairScore = -1f;
			for(int i=0; i<remainingPairs.length; i++)
			{
				Pair pair = remainingPairs[i];
				if(pair == null)
				{
					continue;
				}
				pair.scoreAvailable(otherPoints, bestPairScore);
				if(pair.bestScore > bestPairScore)
				{
					bestPair = pair;
					bestPairIndex = i;
					bestPairScore = pair.bestScore;
				}
			}
			
			// Eat that pair and its points, and add to total score
			remainingPairs[bestPairIndex] = null;
			int aIndex = bestPair.bestAIndex, bIndex = bestPair.bestBIndex;
			otherPoints[aIndex] = null;
			otherPoints[bIndex] = null;
			totalScore += bestPairScore;
			pairsLeft--;
			pointsLeft-=2;
		}
		
		// Scale score (it is now up to 2 * max * number of pairs matched)
		totalScore /= 2 * maxScore * (drawnPairs.length - pairsLeft);
		
		// Score penalty if the number of pairs is different
		int drawnLength = drawnPairs.length;
		while(drawnLength < otherPairs.length)
		{
			totalScore *= SCOREMULTI_WRONG_COUNT;
			drawnLength++;
		}
		while(drawnLength > otherPairs.length)
		{
			totalScore *= SCOREMULTI_WRONG_COUNT;
			drawnLength--;
		}
		
		// Return as percentage
		return totalScore * 100f;
	}
}
