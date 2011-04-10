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
package com.leafdigital.kanji.android;

import java.io.*;
import java.util.*;

import android.content.res.AssetManager;

/**
 * Input stream that reads from multiple assets, joining them together. This
 * allows assets to exceed the 1MB length limit.
 */
public class MultiAssetInputStream extends InputStream
{
	private AssetManager assets;
	private LinkedList<String> remainingFileNames;

	private InputStream current;

	public MultiAssetInputStream(AssetManager assets, String[] fileNames)
	{
		this.assets = assets;
		this.remainingFileNames = new LinkedList<String>(Arrays.asList(fileNames));
	}

	private void checkLoaded() throws IOException
	{
		if(current == null && !remainingFileNames.isEmpty())
		{
			String name = remainingFileNames.removeFirst();
			current = assets.open(name, AssetManager.ACCESS_STREAMING);
		}
	}

	protected void finalize() throws Throwable
	{
		close();
	}

	@Override
	public void close() throws IOException
	{
		if(current != null)
		{
			current.close();
		}
	}

	@Override
	public int read() throws IOException
	{
		checkLoaded();
		if(current == null)
		{
			return -1;
		}
		int value = current.read();
		if(value == -1)
		{
			// EOF, recurse with next stream
			current.close();
			current = null;
			return read();
		}
		else
		{
			return value;
		}
	}

	@Override
	public int read(byte[] b) throws IOException
	{
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int offset, int length) throws IOException
	{
		checkLoaded();
		if(current == null)
		{
			return -1;
		}
		int read = current.read(b, offset, length);
		if(read == -1)
		{
			// EOF, recurse with next stream
			current.close();
			current = null;
			return read(b, offset, length);
		}
		else
		{
			return read;
		}
	}
}
