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

import java.awt.BorderLayout;
import java.lang.reflect.InvocationTargetException;

import javax.swing.*;

/**
 * Frame for testing the kanji recogniser as an application.
 */
public class ExampleApp extends JFrame
{
	/**
	 * Constructor
	 */
	public ExampleApp()
	{
		super("Kanji recognition test");
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new ExamplePanel(getLayeredPane(), null));
		
		pack();
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
	 * @param args No parameters
	 * @throws InterruptedException Invoke error
	 * @throws InvocationTargetException Invoke error
	 */
	public static void main(String[] args) throws InterruptedException, InvocationTargetException
	{
		SwingUtilities.invokeAndWait(new Runnable() 
		{
			@Override
			public void run()
			{
				new ExampleApp();
			}
		});
	}
}
