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
import java.awt.image.BufferedImage;
import java.util.LinkedList;

import javax.swing.*;

import com.leafdigital.kanji.InputStroke;

/** 
 * Swing component for entering kanji characters.
 */
public class KanjiPanel extends JPanel
{
	private Drawing drawing;
	private Handler handler;
	private JButton undo, clear;
	
	private static class UndoEntry
	{
		private InputStroke[] strokes;
		private BufferedImage image;
		private UndoEntry(InputStroke[] strokes, BufferedImage image)
		{
			this.strokes = strokes;
			this.image = image;
		}
	}
	
	private static int UNDO_BUFFER_SIZE = 10;
	
	private class Drawing extends JComponent implements MouseListener, MouseMotionListener
	{
		private InputStroke[] currentStrokes;
		private BufferedImage currentImage;
		private Graphics2D currentGraphics;
		private LinkedList<UndoEntry> undoBuffer = new LinkedList<UndoEntry>();
		
		private int startX, startY, lastX, lastY;
		
		private Drawing()
		{
			setPreferredSize(new Dimension(250, 250));
			addMouseListener(this);
			addMouseMotionListener(this);
			setOpaque(true);
		}
		
		private void undo()
		{
			UndoEntry entry = undoBuffer.removeLast();
			currentStrokes = entry.strokes;
			currentImage = entry.image;
			if(undoBuffer.isEmpty())
			{
				undo.setEnabled(false);
			}
			repaint();
			handler.kanjiChanged(currentStrokes);
		}
		
		private void clear()
		{
			Insets i = getInsets();
			int w = getWidth() - i.left - i.right, h = getHeight() - i.top - i.bottom;
			initImage(w, h);
			repaint();
			handler.kanjiChanged(currentStrokes);
		}
		
		private void initGraphics()
		{
			currentGraphics.setRenderingHint(
				RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			currentGraphics.setColor(SystemColor.textText);
			currentGraphics.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND,
				BasicStroke.JOIN_ROUND));
		}
		
		private void initImage(int w, int h)
		{
			currentImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			currentGraphics = currentImage.createGraphics();
			currentGraphics.setColor(SystemColor.text);
			currentGraphics.fillRect(0, 0, w, h);
			initGraphics();
			undoBuffer.clear();
			undo.setEnabled(false);
			clear.setEnabled(false);
			if(currentStrokes == null || currentStrokes.length != 0)
			{
				currentStrokes = new InputStroke[0];
				handler.kanjiChanged(currentStrokes);
			}
		}

		@Override
		public void setBounds(int x, int y, int width, int height)
		{
			super.setBounds(x, y, width, height);
			Insets i = getInsets();
			int w = width - i.left - i.right, h = height - i.top - i.bottom;
			if(currentImage == null || currentImage.getWidth() != w 
				|| currentImage.getHeight() != h)
			{
				initImage(w, h);
			}
		}
		
		@Override
		protected void paintComponent(Graphics g)
		{
			super.paintComponent(g);
			Insets i = getInsets();
			g.drawImage(currentImage, i.left, i.top, null);
		}
		
		@Override
		public void mousePressed(MouseEvent e)
		{
			// Store the old state in undo buffer
			UndoEntry entry = new UndoEntry(currentStrokes, currentImage);
			undoBuffer.addLast(entry);
			if(undoBuffer.size() > UNDO_BUFFER_SIZE)
			{
				undoBuffer.removeFirst();
			}
			
			// Make a new image
			BufferedImage newImage = new BufferedImage(currentImage.getWidth(),
				currentImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			currentGraphics = newImage.createGraphics();
			currentGraphics.drawImage(currentImage, 0, 0, null);
			currentImage = newImage;
			initGraphics();
			
			// Draw a splodge here and remember this position
			Insets i = getInsets();
			startX = e.getX() - i.left;
			startY = e.getY() - i.top;
			lastX = startX;
			lastY = startY;
			currentGraphics.drawLine(startX, startY, lastX, lastY);
			repaint();
		}
		
		@Override
		public void mouseDragged(MouseEvent e)
		{
			Insets i = getInsets();
			int x = e.getX() - i.left, y = e.getY() - i.top;
			currentGraphics.drawLine(lastX, lastY, x, y);
			repaint();
			lastX = x;
			lastY = y;
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			Insets i = getInsets();
			int x = e.getX() - i.left, y = e.getY() - i.top;
			if(x != lastX || y != lastY)
			{
				mouseDragged(e);
			}
			
			InputStroke stroke = new InputStroke(startX, startY, lastX, lastY);
			InputStroke[] newStrokes = new InputStroke[currentStrokes.length + 1];
			System.arraycopy(currentStrokes, 0, newStrokes, 0, currentStrokes.length);
			newStrokes[currentStrokes.length] = stroke;
			currentStrokes = newStrokes;

			undo.setEnabled(true);
			clear.setEnabled(true);

			handler.kanjiChanged(newStrokes);
		}
		
		@Override
		public void mouseClicked(MouseEvent e)
		{
		}
		@Override
		public void mouseEntered(MouseEvent e)
		{
		}
		@Override
		public void mouseExited(MouseEvent e)
		{
		}
		@Override
		public void mouseMoved(MouseEvent e)
		{
		}
	}
	
	/**
	 * Interface for something that handles kanji panel information.
	 */
	public interface Handler
	{
		/**
		 * Called every time the kanji changes.
		 * @param strokes New strokes
		 */
		public void kanjiChanged(InputStroke[] strokes);
	}
	
	/**
	 * @param handler Handler for events
	 */
	public KanjiPanel(Handler handler)
	{
		this.handler = handler;
		setOpaque(false);
	
		setLayout(new BorderLayout());
		
		JPanel buttons = new JPanel(new GridLayout(1, 2));
		add(buttons, BorderLayout.SOUTH);
		buttons.setOpaque(false);
		
		undo = new JButton("Undo");
		undo.setEnabled(false);
		buttons.add(undo);
		undo.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				drawing.undo();
			}
		});
		
		clear = new JButton("Clear");
		clear.setEnabled(false);
		buttons.add(clear);
		clear.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				drawing.clear();
			}
		});
	
		drawing = new Drawing();
		add(drawing, BorderLayout.CENTER);
	}
	
	/**
	 * Clears the drawing.
	 */
	public void clear()
	{
		drawing.clear();
	}
}
