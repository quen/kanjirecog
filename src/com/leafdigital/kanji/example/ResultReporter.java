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
import java.awt.image.*;

import javax.swing.*;

import com.leafdigital.kanji.*;
import com.leafdigital.kanji.KanjiInfo.MatchAlgorithm;
import com.leafdigital.kanji.example.ResultsPanel.Handler;

/** 
 * Class that reports results to leafdigital server for statistical analysis.
 */
public class ResultReporter implements Handler
{
	private KanjiPanel kanjiPanel;
	private JLayeredPane layeredPane;
	private Handler handler;

	/**
	 * Handler for anyone who wants to be notified.
	 */
	public interface Handler
	{
		/**
		 * Called when a new kanji is selected.
		 * @param kanji Kanji
		 */
		public void newKanji(String kanji);
	}
	 
	/**
	 * @param layeredPane Pane for showing save progress
	 * @param handler Callback handler
	 */
	public ResultReporter(JLayeredPane layeredPane, Handler handler)
	{
		this.layeredPane = layeredPane;
		this.handler = handler;
	}

	@Override
	public void clickedKanji(KanjiInfo drawing, String kanji, 
		MatchAlgorithm algo, int ranking)
	{
		StatsReporter.phoneHome(drawing, kanji, algo, ranking, "Example applet 1.0",
			new StatsReporter.Callback()
		{
			@Override
			public void phoneHomeStart()
			{
				startSave();
			}
			
			@Override
			public void phoneHomeEnd(boolean ok)
			{
				endSave(ok);
			}
		});
		kanjiPanel.clear();
		handler.newKanji(kanji);
	}

	private class SaveNote extends JComponent
	{
		private float opacity = 0f;
		private String text = null;
		private boolean fadingIn, red;
		
		private BufferedImage image;
		
		@Override
		protected void paintComponent(Graphics g)
		{
			if(text == null)
			{
				return;
			}
			
			// Create new image if needed
			if(image == null || image.getWidth() != getWidth() 
				|| image.getHeight() != getHeight())
			{
				image = new BufferedImage(getWidth(), getHeight(),
					BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2 = image.createGraphics();
				g2.setRenderingHint(
					RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				
				// Draw triangle
				g2.setColor(red ? new Color(128, 0, 0) : SystemColor.textText);
				g2.fillPolygon(new int[] { 0, getWidth(), 0 },
					new int[] {0, 0, getHeight()}, 3);
				
				// Draw text
				g2.setColor(red ? Color.WHITE : SystemColor.text);
				Font font = new Font("Verdana", Font.BOLD, 9);
				g2.setFont(font);
				g2.translate(getWidth() / 2, getHeight() / 2);
				g2.rotate(-Math.PI / 4.0);
				FontMetrics metrics = g2.getFontMetrics(font);
				int width = metrics.stringWidth(text);
				
				g2.drawString(text, -width/2, -metrics.getDescent());
			}
			
			// Draw with opacity
			RescaleOp rop = new RescaleOp(
				new float[] { 1f, 1f, 1f, opacity }, new float[4], null);
			((Graphics2D)g).drawImage(image, rop, 0, 0);
		}
		
		@Override
		public Dimension getPreferredSize()
		{
			return new Dimension(50, 50);
		}
		
		private void fadeIn(String text)
		{
			// Set text
			if(this.text != null)
			{
				throw new IllegalStateException();
			}
			this.text = text;
			fadingIn = true;
			
			// Start thread
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					// Fade in
					while(opacity < 0.9999f)
					{
						opacity += 0.05f;
						repaint();
						try
						{
							Thread.sleep(25);
						}
						catch(InterruptedException ie)
						{
						}
					}
					opacity = 1.0f;
					repaint();
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							fadingIn = false;
						}
					});
				}
			}).start();
		}
		
		private void fadeOut(final String newText, final boolean red)
		{
			// Start thread
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					// If it's still fading in, wait
					while(fadingIn)
					{
						try
						{
							Thread.sleep(10);
						}
						catch(InterruptedException ie)
						{
						}
					}
					
					// Fade out fast
					while(opacity > 0.0001f)
					{
						opacity -= 0.05f;
						repaint();
						try
						{
							Thread.sleep(10);
						}
						catch(InterruptedException ie)
						{
						}
					}
					opacity = 0.0f;
					text = newText;
					image = null;
					SaveNote.this.red = red;
					
					// Fade in fast
					while(opacity < 0.9999f)
					{
						opacity += 0.05f;
						repaint();
						try
						{
							Thread.sleep(10);
						}
						catch(InterruptedException ie)
						{
						}
					}
					opacity = 1.0f;
					
					// Fade out slow
					while(opacity > 0.0001f)
					{
						opacity -= 0.01f;
						repaint();
						try
						{
							Thread.sleep(25);
						}
						catch(InterruptedException ie)
						{
						}
					}
					opacity = 0f;
					
					// Remove
					SwingUtilities.invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							getParent().remove(SaveNote.this);
							synchronized(this)
							{
								saveNote = null;
							}
						}
					});
				}
			}).start();
		}
	}
	
	private SaveNote saveNote;
	
	private synchronized void startSave()
	{
		if(saveNote != null)
		{
			return;
		}
		
		// Always occurs in UI thread
		saveNote = new SaveNote();
		layeredPane.add(saveNote, JLayeredPane.POPUP_LAYER);
		saveNote.setBounds(0,	0, 
			saveNote.getPreferredSize().width, saveNote.getPreferredSize().height);
		saveNote.fadeIn("Sending...");
	}
	
	private synchronized void endSave(final boolean ok)
	{
		if(saveNote == null)
		{
			return;
		}
		
		// Occurs in other thread
		SwingUtilities.invokeLater(new Runnable()
		{
			@Override
			public void run()
			{
				saveNote.fadeOut(ok ? "Sent OK" : "Failed", !ok);
			}
		});
	}

	/**
	 * @param kanjiPanel Kanji panel (will be cleared on select)
	 */
	public void setKanjiPanel(KanjiPanel kanjiPanel)
	{
		this.kanjiPanel = kanjiPanel;
	}
}
