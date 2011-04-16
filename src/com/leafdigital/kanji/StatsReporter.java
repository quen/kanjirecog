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
import java.net.*;

import com.leafdigital.kanji.KanjiInfo.MatchAlgorithm;

/**
 * Used for reporting information about how the kanji was drawn to the
 * leafdigital server so that we can use it for statistical information
 * to improve the service in future.
 */
public abstract class StatsReporter
{
	private static final String baseUrl =
		"http://live.leafdigital.com/kanji/report.jsp";

	/**
	 * Interface you can implement if you want to get information about the
	 * phone-home process.
	 */
	public interface Callback
	{
		/**
		 * Called when phone-home is starting.
		 */
		public void phoneHomeStart();

		/**
		 * Called when phone-home ends.
		 * @param ok True if it was successful
		 */
		public void phoneHomeEnd(boolean ok);
	}

	/**
	 * Phones home with information about the kanji match.
	 * @param drawn Drawn kanji (should include the strokes that the user drew;
	 *   this is the drawn kanji that you matched against other kanjis)
	 * @param kanji Final selected kanji as string
	 * @param algo Match algorithm that the user chose the final kanji from
	 * @param ranking Ranking of kanji within selected match algorithm
	 * @param clientName Client name (name of this client! Can be anything (up
	 *   to 255 characters); include version if you like
	 * @param callback Optional callback function for information about
	 *   phone-home process; null if not required
	 */
	public static void phoneHome(KanjiInfo drawn, String kanji, MatchAlgorithm algo,
		int ranking, String clientName, Callback callback)
	{
		try
		{
			new SendThread(baseUrl, "drawing=" + drawn.getFullSummary() + "&kanji=" + kanji
				+ "&algo=" + algo + "&ranking=" + ranking + "&clientname=" +
				URLEncoder.encode(clientName, "UTF-8"), callback);
		}
		catch(UnsupportedEncodingException e)
		{
			// Can't happen
			throw new Error(e);
		}
	}

	private static class SendThread extends Thread
	{
		private Callback callback;
		private String url, post;

		private SendThread(String url, String post, Callback callback)
		{
			this.url = url;
			this.post = post;
			this.callback = callback;
			if(callback != null)
			{
				callback.phoneHomeStart();
			}
			start();
		}

		@Override
		public void run()
		{
			boolean ok = false;
			try
			{
				URL u = new URL(url);
				HttpURLConnection conn = (HttpURLConnection)u.openConnection();
				conn.setRequestMethod("POST");
				conn.setDoOutput(true);
				conn.setConnectTimeout(10000);
				conn.setReadTimeout(10000);
				conn.connect();
				OutputStream outputStream = conn.getOutputStream();
				outputStream.write(post.getBytes("UTF-8"));
				outputStream.close();

				InputStream input;
				try
				{
					input = conn.getInputStream();
				}
				catch(IOException e)
				{
					input = conn.getErrorStream();
				}
				if(input != null)
				{
					BufferedReader reader = new BufferedReader(
						new InputStreamReader(input, "UTF-8"));

					String firstLine = reader.readLine();
					if(firstLine == null)
					{
						// Empty response
					}
					else if(firstLine.equals("OK"))
					{
						ok = true;
					}

					reader.close();
				}
			}
			catch(IOException e)
			{
			}
			finally
			{
				if(callback != null)
				{
					callback.phoneHomeEnd(ok);
				}
			}
		}
	}
}
