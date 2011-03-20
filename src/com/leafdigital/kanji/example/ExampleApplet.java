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

import java.applet.Applet;
import java.awt.BorderLayout;
import java.lang.reflect.Method;

import javax.swing.JApplet;

/**
 * Applet for testing the recogniser.
 */
public class ExampleApplet extends JApplet
{
	/**
	 * Constructor
	 */
	@Override
	public void start()
	{
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new ExamplePanel(getLayeredPane(), new ResultReporter.Handler()
		{
			@Override
			public void newKanji(String kanji)
			{
				// Pass kanji to JavaScript. This is needed because users can't copy
				// /paste from unsigned Java applets.
				try
				{
					Class<?> c = Class.forName("netscape.javascript.JSObject");
					Method m = c.getMethod("getWindow", Applet.class);
					// window = JSObject.getWindow(this);
					Object window = m.invoke(null, ExampleApplet.this);
					m = c.getMethod("eval", String.class);
					// window.eval("addKanji('whatever');");
					m.invoke(window, "addKanji('" + kanji + "');");
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}));
	}
}
