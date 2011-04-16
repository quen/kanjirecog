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

import java.util.*;

import android.content.*;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.leafdigital.kanji.*;
import com.leafdigital.kanji.android.KanjiDrawing.DrawnStroke;

public class TopResultsActivity extends KanjiActivity
{
	/**
	 * Intent key that should contain an int from R.strings with the activity
	 * label.
	 */
	public final static String EXTRA_LABEL = "label";

	/**
	 * Intent key that should contain a string array with possible match kanji
	 * (first in array = best match).
	 */
	public final static String EXTRA_MATCHES = "matches";

	/**
	 * Intent key that should contain an int from R.strings with the 'not one of
	 * these' button label.
	 */
	public final static String EXTRA_OTHERLABEL = "otherlabel";

	/**
	 * Intent key that should contain an int indicating which result in the
	 * matches array to start from.
	 */
	public final static String EXTRA_STARTFROM = "startfrom";

	/**
	 * Intent key that should contain an boolean; true indicates that the smaller
	 * kanji grid is used.
	 */
	public final static String EXTRA_SHOWMORE = "showmore";

	/**
	 * Intent key that should contain an array of strings for kanji which were
	 * already shown and should be skipped.
	 */
	public final static String EXTRA_ALREADYSHOWN = "alreadyshown";

	/**
	 * Current algorithm used in intent.
	 */
	public final static String EXTRA_ALGO = "algo";

	/**
	 * Number of kanji shown in top count screen.
	 */
	public final static int TOP_COUNT = 7;

	/**
	 * Number of kanji shown in more count screen.
	 */
	public final static int MORE_COUNT = 12;

	private final static int[] ALL_IDS =
	{
		R.id.no1, R.id.no2, R.id.no3, R.id.no4, R.id.no5, R.id.no6,
		R.id.no7, R.id.no8, R.id.no9, R.id.no10, R.id.no11, R.id.no12
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final DrawnStroke[] strokes = DrawnStroke.loadFromIntent(getIntent());

		String[] matches = getIntent().getStringArrayExtra(EXTRA_MATCHES);
		HashSet<String> shown = new HashSet<String>(Arrays.asList(
			getIntent().getStringArrayExtra(EXTRA_ALREADYSHOWN)));
		int startFrom = getIntent().getIntExtra(EXTRA_STARTFROM, 0);
		int label = getIntent().getIntExtra(EXTRA_LABEL, 0);
		int otherLabel = getIntent().getIntExtra(EXTRA_OTHERLABEL, 0);
		boolean showMore = getIntent().getBooleanExtra(EXTRA_SHOWMORE, false);
		final KanjiInfo.MatchAlgorithm algo =
			KanjiInfo.MatchAlgorithm.valueOf(getIntent().getStringExtra(EXTRA_ALGO));

		setTitle(getString(label).replace("#", strokes.length + ""));
		setContentView(showMore ? R.layout.moreresults : R.layout.topresults);
		((Button)findViewById(R.id.other)).setText(getString(otherLabel));

		int[] ids = new int[showMore ? MORE_COUNT : TOP_COUNT];
		System.arraycopy(ALL_IDS, 0, ids, 0, ids.length);

		int index = -startFrom;
		int buttonIndex = 0;
		for(int match=0; match<matches.length; match++)
		{
			// Skip matches we already showed
			if(shown.contains(matches[match]))
			{
				continue;
			}

			// See if this is one to draw
			if(index >= 0)
			{
				Button button = (Button)findViewById(ids[buttonIndex++]);
				button.setText(matches[match]);
				final Intent data = new Intent();
				final int ranking = match + 1;
				data.putExtra(PickKanjiActivity.EXTRA_KANJI, matches[match]);
				button.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						setResult(RESULT_OK, data);

						// If selected, report stats
						SharedPreferences prefs =
							PreferenceManager.getDefaultSharedPreferences(
								TopResultsActivity.this);
						if(prefs.getBoolean(MainActivity.PREF_REPORTSTATS, false))
						{
							// If the user has a network connection, send stats
							ConnectivityManager cm = (ConnectivityManager) getSystemService(
								Context.CONNECTIVITY_SERVICE);
							if(cm != null && cm.getActiveNetworkInfo() != null
								&& cm.getActiveNetworkInfo().isConnected())
							{
								StatsReporter.phoneHome(PickKanjiActivity.getKanjiInfo(strokes),
									data.getStringExtra(PickKanjiActivity.EXTRA_KANJI),
									algo, ranking, "leafdigital Kanji Draw 0.8", null);
							}
						}

						finish();
					}
				});

				// Stop if we filled all the buttons
				if(buttonIndex >= ids.length)
				{
					break;
				}
			}

			index++;
		}

		// Clear all the unused buttons
		for(; buttonIndex<ids.length; buttonIndex++)
		{
			Button button = (Button)findViewById(ids[buttonIndex]);
			button.setText(" ");
			button.setEnabled(false);
		}

		Button button = (Button)findViewById(R.id.other);
		button.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if(!PickKanjiActivity.tryMore(TopResultsActivity.this, getIntent()))
				{
					setResult(RESULT_OK);
					finish();
				}
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(checkQuit(data))
		{
			return;
		}
		if(resultCode == RESULT_OK)
		{
			setResult(RESULT_OK, data);
			finish();
		}
	}
}
