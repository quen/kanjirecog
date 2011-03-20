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
package com.leafdigital.kanji.example;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import com.leafdigital.kanji.*;
import com.leafdigital.kanji.KanjiInfo.MatchAlgorithm;

/** 
 * Panel for displaying resulting kanji.
 */
public class ResultsPanel extends JPanel implements KanjiPanel.Handler
{
	/**
	 * Font used on Mac. 
	 */
	private static final String MAC_FONT = "Hiragino Kaku Gothic Pro";

	private KanjiList kanjiList;
	
	private Font smallFont = new Font("Verdana", Font.BOLD, 10);
	private KanjiBox[] boxes;
//	private KanjiBox other;
//	private String currentStrokes;
	private KanjiInfo currentDrawing;
	
	private Handler handler;
	
	private KanjiInfo.MatchAlgorithm algo;

	/**
	 * Interface for something that wants to receive information from this panel.
	 */
	public interface Handler
	{
		/**
		 * Called when user clicks on a kanji.
		 * @param drawing Kanji that was drawn
		 * @param kanji Kanji clicked
		 * @param algo Algorithm
		 * @param ranking Ranking of kanji (1 = first)
		 */
		public void clickedKanji(KanjiInfo drawing, String kanji,
			MatchAlgorithm algo, int ranking);
//		
//		/**
//		 * Called when user clicks the 'not here guv' option.
//		 * @param strokes String of all strokes the user entered
//		 */
//		public void clickedOther(String strokes);
	}
	
	private class KanjiBox extends JComponent
	{
		private boolean showing, mouseover;
		private String currentKanji = null;
		
		protected JLabel kanji = new JLabel("\u4e00");
		protected JLabel score = new JLabel("100.0%");
		
		private KanjiBox(final int ranking)
		{
			setLayout(new BorderLayout());

			setBackground(SystemColor.text);
			setOpaque(true);
			
			add(kanji, BorderLayout.CENTER);
			kanji.setHorizontalAlignment(SwingConstants.CENTER);
			kanji.setForeground(SystemColor.textText);
			kanji.setVerticalAlignment(SwingConstants.TOP);
			
			add(score, BorderLayout.NORTH);
			score.setHorizontalAlignment(SwingConstants.CENTER);
			score.setFont(smallFont);
			// 50% background
			Color background = new Color(
				(SystemColor.textText.getRed() + SystemColor.text.getRed()) / 2,
				(SystemColor.textText.getGreen() + SystemColor.text.getGreen()) / 2,
				(SystemColor.textText.getBlue() + SystemColor.text.getBlue()) / 2);
			score.setBackground(background);
			score.setForeground(SystemColor.text);
			score.setOpaque(true);
			
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseEntered(MouseEvent e)
				{
					mouseover = true;
					repaint();
				}
				@Override
				public void mouseExited(MouseEvent e)
				{
					mouseover = false;
					repaint();
				}
				@Override
				public void mouseClicked(MouseEvent e)
				{
					if(ranking == -1)
					{
//						handler.clickedOther(currentStrokes);
					}
					else
					{
						handler.clickedKanji(currentDrawing, currentKanji, algo, ranking);
					}
				}
			});
		}
		
		@Override
		protected void paintComponent(Graphics g)
		{
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			if(mouseover && showing)
			{
				((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(new Color(255, 220, 220));
				int y = score.getHeight();
				int height = getHeight() - y;
				int width = getWidth();
				int x = 0;
				if(width > height)
				{
					x = (width-height) / 2;
					width = height;
				}
				g.fillOval(x, y, width, height);
			}
		}
		
		@Override
		public void setFont(Font font)
		{
			kanji.setFont(font);
		}
		
		public void setKanji(KanjiMatch match)
		{
			if(match == null)
			{
				setSpecialVisible(false);
			}
			else
			{
				setSpecialVisible(true);
				currentKanji = match.getKanji().getKanji();
				kanji.setText(currentKanji);
				score.setText(String.format("%.1f", match.getScore()) + "%");
			}
		}
		
		protected void setSpecialVisible(boolean showing)
		{
			if(this.showing != showing)
			{
				this.showing = showing;
				repaint();
			}
		}
		
		@Override
		protected void paintChildren(Graphics g)
		{
			if(showing)
			{
				super.paintChildren(g);
			}
		}
		
		@Override
		public Dimension getPreferredSize()
		{
			Dimension result = new Dimension();
			result.width = Math.max(
				getFontMetrics(kanji.getFont()).stringWidth("\u4e00"),
				getFontMetrics(score.getFont()).stringWidth("100.0%"));
			FontMetrics metrics = kanji.getFontMetrics(kanji.getFont());
			if(kanji.getFont().getFamily().equals(MAC_FONT))
			{
				// On Mac, the spacing is weird - it allows for descender or something
				result.height = score.getPreferredSize().height + 
				  metrics.getAscent() + metrics.getLeading() / 4;
			}
			else
			{
				result.height = score.getPreferredSize().height + 
					metrics.getAscent() + metrics.getLeading();
			}
			return result;
		}
	}
	/*
	private class OtherBox extends KanjiBox
	{
		OtherBox()
		{
			super(-1);
			score.setText("?");
			kanji.setFont(smallFont);
			kanji.setText("Something else");
		}
		
		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(Math.max(kanji.getPreferredSize().width,
				score.getPreferredSize().width), 
				kanji.getPreferredSize().height +	score.getPreferredSize().height);
		}
	}
	*/
	
	/**
	 * @param kanjiList Kanji list
	 * @param title Title of panel
	 * @param algo Match algorithm
	 * @param handler Handler for when something is selected
	 */
	public ResultsPanel(KanjiList kanjiList, String title,
		KanjiInfo.MatchAlgorithm algo, Handler handler)
	{
		this.kanjiList = kanjiList;
		this.algo = algo;
		this.handler = handler;
		setLayout(new BorderLayout(4, 4));
		setOpaque(false);

		JLabel heading = new JLabel(title);
		heading.setFont(smallFont);
		add(heading, BorderLayout.NORTH);
		
		JPanel outer = new JPanel(new BorderLayout(4, 4));
		add(outer, BorderLayout.CENTER);
		
		// Create the boxes for showing results
		boxes = new KanjiBox[11];
		for(int i=0; i<boxes.length; i++)
		{
			boxes[i] = new KanjiBox(i+1);
		}
		
		// Add pattern: 1x1, 2x2, 3x2.
		boxes[0].setFont(getSuitableFont(120));
		outer.add(boxes[0], BorderLayout.NORTH);
		
		Font f = getSuitableFont(60);
		JPanel next = new JPanel(new BorderLayout(4, 4));
		next.setOpaque(false);
		outer.add(next, BorderLayout.CENTER);
		JPanel grid = new JPanel(new GridLayout(2, 2, 4, 4));
		grid.setOpaque(false);
		next.add(grid, BorderLayout.NORTH);
		for(int i=0; i<4; i++)
		{
			boxes[i+1].setFont(f);
			grid.add(boxes[i+1]);
		}
		
		f = getSuitableFont(40);
		JPanel next2 = new JPanel(new BorderLayout(4, 4));
		next2.setOpaque(false);
		next.add(next2, BorderLayout.CENTER);
		grid = new JPanel(new GridLayout(2, 3, 4, 4));
		grid.setOpaque(false);
		next2.add(grid, BorderLayout.NORTH);
		for(int i=0; i<6; i++)
		{
			boxes[i+5].setFont(f);
			grid.add(boxes[i+5]);
		}
		
		// Add Not shown button
/*		other = new OtherBox();
		JPanel next3 = new JPanel(new BorderLayout(4, 4));
		next3.setOpaque(false);
		next2.add(next3, BorderLayout.CENTER);
		next3.add(other, BorderLayout.NORTH);*/
	}

	private static Font getSuitableFont(int size)
	{	
		Font result = new Font(MAC_FONT, Font.PLAIN, size);
		if(result.getFamily().equals(MAC_FONT))
		{
			return result;
		}
		result = new Font("MS Gothic", Font.PLAIN, size);
		return result;
	}

	@Override
	public void kanjiChanged(InputStroke[] strokes)
	{
		new SearchThread(strokes);
	}
	
	private void updateResults(KanjiInfo potentialKanji, KanjiMatch[] matches)
	{
		int count = 0;
		for(KanjiMatch match : matches)
		{
			boxes[count++].setKanji(match);
			if(count == boxes.length)
			{
				break;
			}
		}
//		other.setSpecialVisible(count > 0);
		for(;count<boxes.length; count++)
		{
			boxes[count].setKanji(null);
		}
		this.currentDrawing = potentialKanji;
	}
	
	private class SearchThread extends Thread
	{
		private KanjiInfo potentialKanji;
		
		private SearchThread(InputStroke[] strokes)
		{
			potentialKanji = new KanjiInfo("?");
			for(InputStroke stroke : strokes)
			{
				potentialKanji.addStroke(stroke);
			}
			potentialKanji.finish();
//			currentStrokes = potentialKanji.getAllDirections();
			start();
		}
		
		@Override
		public void run()
		{
			final KanjiMatch[] matches = kanjiList.getTopMatches(potentialKanji, algo);
			SwingUtilities.invokeLater(new Runnable()
			{
				@Override
				public void run()
				{
					updateResults(potentialKanji, matches);
				}
			});
		}
		
	}
}
