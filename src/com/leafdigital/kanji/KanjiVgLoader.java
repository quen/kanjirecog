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

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads kanji stroke order data from a file in KanjiVG format and simplifies
 * it into the basic stroke data used for recognition.
 * http://kanjivg.tagaini.net/
 */
public class KanjiVgLoader
{
	private InputStream input;
	private LinkedList<KanjiInfo> read = new LinkedList<KanjiInfo>();
	private LinkedList<String> warnings = new LinkedList<String>();
	private HashSet<Integer> done = new HashSet<Integer>();

	/**
	 * SAX handler.
	 */
	private class Handler extends DefaultHandler
	{
		private KanjiInfo current = null;

		@Override
		public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException
		{
			if(qName.equals("kanji"))
			{
				// Clear current just in case
				current = null;

				// Note: I used the midashi attribute initially, but had problems
				// with the parser bizarrely misinterpreting some four-byte sequences.
				String id = attributes.getValue("id");
				if(id == null)
				{
					warnings.add("<kanji> tag missing id=");
					return;
				}
				int codePoint;
				try
				{
					codePoint = Integer.parseInt(id, 16);
				}
				catch(NumberFormatException e)
				{
					warnings.add("<kanji> tag invalid id= (" + id + ")");
					return;
				}
				if(!done.add(codePoint))
				{
					warnings.add("<kanji> duplicate id= (" + id + ")");
					return;
				}

				// Check if code point is actually a CJK ideograph
				String kanjiString = new String(Character.toChars(codePoint));
				if((codePoint >= 0x4e00 && codePoint <= 0x9fff)
					|| (codePoint >= 0x3400 && codePoint <= 0x4dff)
					|| (codePoint >= 0x20000 && codePoint <= 0x2a6df)
					|| (codePoint >= 0xf900 && codePoint <= 0xfaff)
					|| (codePoint >= 0x2f800 && codePoint <= 0x2fa1f))
				{
					current = new KanjiInfo(kanjiString);
				}
				else
				{
					// Ignore non-kanji characters
					return;
				}
			}
			else if(qName.equals("stroke"))
			{
				if(current != null)
				{
					String path = attributes.getValue("path");
					if(path == null)
					{
						warnings.add("<stroke> tag in kanji " +
							current.getKanji() + " missing path=, ignoring kanji");
						current = null;
						return;
					}
					try
					{
						InputStroke stroke = new InputStroke(path);
						current.addStroke(stroke);
					}
					catch(IllegalArgumentException e)
					{
						warnings.add("<stroke> tag in kanji " + current.getKanji() +
							" invalid path= (" + path + "): " + e.getMessage());
						current = null;
						return;
					}
				}
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName)
			throws SAXException
		{
			if(qName.equals("kanji"))
			{
				if(current != null)
				{
					current.finish();
					read.add(current);
				}
			}
		}
	}

	/**
	 * Constructs ready to read data.
	 * @param input Input stream (will be closed after {@link #loadKanji()}
	 *   finishes)
	 */
	public KanjiVgLoader(InputStream input)
	{
		this.input = input;
	}

	/**
	 * Loads all kanji from the file and closes it.
	 * @return All kanji as array
	 * @throws IOException Any error reading data or with format
	 */
	public synchronized KanjiInfo[] loadKanji() throws IOException
	{
		if(input == null)
		{
			throw new IOException("Cannot load kanji more than once");
		}

		// Parse data
		SAXParser parser;
		try
		{
			parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(input, new Handler());
			input.close();
		}
		catch(ParserConfigurationException e)
		{
			IOException x = new IOException("Failed to initialise SAX parser");
			x.initCause(e);
			throw x;
		}
		catch(SAXException e)
		{
			IOException x = new IOException("Failed to parse KanjiVG file");
			x.initCause(e);
			throw x;
		}

		// Return result
		return read.toArray(new KanjiInfo[read.size()]);
	}

	/**
	 * @return All warnings encountered while loading the file
	 */
	public synchronized String[] getWarnings()
	{
		return warnings.toArray(new String[warnings.size()]);
	}

	/**
	 * Convert KanjiVG file into new info file.
	 * @param args Filename to convert and output filename
	 */
	public static void main(String[] args)
	{
		if(args.length < 2 || args.length > 3
			|| (args.length==3 && !args[2].matches("[0-9]{1,5}")))
		{
			System.err.println("Incorrect command line arguments. Syntax:\n"
				+ "KanjiVgLoader <kanjivgfile> <output file> [max size in kb]\n"
				+ "Max size is used to optionally split the file into multiple\n"
				+ "parts.");
			return;
		}

		File in = new File(args[0]);
		if(!in.canRead())
		{
			System.err.println("Unable to read input file: " + args[0]);
			return;
		}

		File out;
		int maxBytes = -1;
		String fileName = args[1];
		if(args.length == 3)
		{
			maxBytes = Integer.parseInt(args[2]) * 1024;

			out = new File(fileName + ".1");
			if(out.exists())
			{
				System.err.println("Output file already exists: " + fileName + ".1");
				return;
			}
		}
		else
		{
			out = new File(fileName);
			if(out.exists())
			{
				System.err.println("Output file already exists: " + fileName);
				return;
			}
		}

		try
		{
			// Load everything
			KanjiVgLoader loader = new KanjiVgLoader(new BufferedInputStream(
				new FileInputStream(in)));
			System.out.println("Loading input file: " + in.getName());
			KanjiInfo[] allKanji = loader.loadKanji();
			System.out.println("Loaded " + allKanji.length + " kanji.");
			System.out.println();
			if(loader.getWarnings().length > 0)
			{
				System.out.println("Warnings:");
				for(String warning : loader.getWarnings())
				{
					System.out.println("  " + warning);
				}
				System.out.println();
			}

			KanjiList list = new KanjiList();
			for(KanjiInfo kanji : allKanji)
			{
				list.add(kanji);
			}

			OutputStream stream;
			if(maxBytes == -1)
			{
				System.out.println("Writing output file: " + out.getName());
				stream = new FileOutputStream(out);
			}
			else
			{
				System.out.println("Writing output files: " + fileName + ".*");
				stream = new SplitOutputStream(fileName, maxBytes);
			}
			list.save(stream, in.getName());
			stream.close();
		}
		catch(IOException e)
		{
			System.err.println("Error processing file: " + e.getMessage());
			System.err.println();
			System.err.println("FULL STACK TRACE:");
			System.err.println();
			e.printStackTrace();
		}
	}

	/**
	 * Output stream capable of writing multiple files; the first has .1, the
	 * second .2, etc.
	 */
	private static class SplitOutputStream extends OutputStream
	{
		private String basePath;
		private int maxBytes;

		private OutputStream out;
		private int bytesLeft;
		private int index;

		private SplitOutputStream(String basePath, int maxBytes) throws IOException
		{
			this.basePath = basePath;
			this.maxBytes = maxBytes;
			index = 0;
		}

		@Override
		public void close() throws IOException
		{
			if(out != null)
			{
				out.close();
				out = null;
			}
		}

		private void checkOutput() throws IOException
		{
			if(out == null)
			{
				index++;
				File file = new File(basePath + "." + index);
				if(file.exists())
				{
					throw new IOException("File already exists: " + file.getPath());
				}
				out = new FileOutputStream(file);
				bytesLeft = maxBytes;
			}
		}

		private void wroteBytes(int wrote) throws IOException
		{
			bytesLeft -= wrote;
			if(bytesLeft <= 0)
			{
				out.close();
				out = null;
			}
		}

		@Override
		public void write(int oneByte) throws IOException
		{
			checkOutput();
			out.write(oneByte);
			wroteBytes(1);
		}

		@Override
		public void write(byte[] buffer, int offset, int count) throws IOException
		{
			checkOutput();
			if(count > bytesLeft)
			{
				int wasLeft = bytesLeft;
				write(buffer, offset, bytesLeft);
				write(buffer, offset + wasLeft, count - wasLeft);
				return;
			}
			out.write(buffer, offset, count);
			wroteBytes(count);
		}

		@Override
		public void write(byte[] buffer) throws IOException
		{
			write(buffer, 0, buffer.length);
		}
	}
}
