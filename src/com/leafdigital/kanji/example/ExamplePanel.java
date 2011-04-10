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
import java.io.*;

import javax.swing.*;

import com.leafdigital.kanji.*;

/** 
 * Main panel that implements test application/applet (so that code can be
 * shared between both).
 */
public class ExamplePanel extends JPanel
{
	private KanjiList list;
	
	/**
	 * Constructs panel and loads kanji list.
	 * @param databaseStream Input stream containing database
	 * @param layeredPane Layered pane
	 * @param resultsHandler Results handler function (used to update text;
	 *   if null, includes text display box)
	 */
	public ExamplePanel(InputStream databaseStream, JLayeredPane layeredPane,
		ResultReporter.Handler resultsHandler)
	{
		super(new BorderLayout(4, 4));
		setOpaque(false);
		
		try
		{
			list = new KanjiList(databaseStream);
		}
		catch(IOException e)
		{
			throw new Error(e);
		}

		final JTextField text;
		if(resultsHandler == null)
		{
			text = new JTextField();
			resultsHandler = new ResultReporter.Handler()
			{
				@Override
				public void newKanji(String kanji)
				{
					text.setText(text.getText() + kanji);
				}
			};
			// Commented this code: it works, but is annoying on OS X where the
			// current input mode is system-wide not per app
//			text.addFocusListener(new FocusAdapter()
//			{
//				@Override
//				public void focusGained(FocusEvent e)
//				{
//					text.getInputContext().selectInputMethod(Locale.JAPANESE);
//				}
//			});
		}
		else
		{
			text = null;
		}

		ResultReporter handler = new ResultReporter(layeredPane, resultsHandler);

		JPanel resultsGrid = new JPanel(new GridLayout(1, 2, 4, 0));
		resultsGrid.setOpaque(false);
		add(resultsGrid, BorderLayout.EAST);
		
		final ResultsPanel results = new ResultsPanel(list, "Exact match",
			KanjiInfo.MatchAlgorithm.STRICT, handler);
		resultsGrid.add(results);

		final ResultsPanel results2 = new ResultsPanel(list, "Fuzzy match",
			KanjiInfo.MatchAlgorithm.FUZZY, handler);
		resultsGrid.add(results2);

		final ResultsPanel results3 = new ResultsPanel(list, "\u00b11 stroke",
			KanjiInfo.MatchAlgorithm.FUZZY_1OUT, handler);
		// Add extra borders to indicate difference in stroke count
		results3.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
		resultsGrid.add(results3);
		
		final ResultsPanel results4 = new ResultsPanel(list, "\u00b12 strokes",
			KanjiInfo.MatchAlgorithm.FUZZY_2OUT, handler);
		resultsGrid.add(results4);
		
		JPanel middle = new JPanel(new BorderLayout(4, 4));
		middle.setOpaque(false);
		add(middle, BorderLayout.CENTER);
		
		KanjiPanel kanjiPanel = new KanjiPanel(new KanjiPanel.Handler()
		{
			@Override
			public void kanjiChanged(InputStroke[] strokes)
			{
				results.kanjiChanged(strokes);
				results2.kanjiChanged(strokes);
				results3.kanjiChanged(strokes);
				results4.kanjiChanged(strokes);
			}
		});
		middle.add(kanjiPanel, BorderLayout.CENTER);
		
		if(text != null)
		{
			JPanel textPanel = new JPanel(new BorderLayout());
			textPanel.setOpaque(false);
			textPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			textPanel.add(new JLabel("Result for cut/paste:"), BorderLayout.NORTH);
			textPanel.add(text, BorderLayout.SOUTH);
			middle.add(textPanel, BorderLayout.SOUTH);
		}

		handler.setKanjiPanel(kanjiPanel);
	}
}
