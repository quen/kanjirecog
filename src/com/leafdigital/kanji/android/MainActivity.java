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

import android.content.*;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

public class MainActivity extends KanjiActivity
	implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private static final String PREF_SHOWNAVIGATION = "shownotification";
	private static final String PREF_STARTWITHSYSTEM = "startwithsystem";
	/**
	 * If true, reports stats
	 */
	public static final String PREF_REPORTSTATS = "reportstats";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		OnClickListener onClickListener = new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivityForResult(
					new Intent(MainActivity.this, PickKanjiActivity.class), 0);
			}
		};
		((Button)findViewById(R.id.drawkanji)).setOnClickListener(onClickListener);

		((Button)findViewById(R.id.copy)).setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				ClipboardManager clipboard =
					(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
				EditText edit = (EditText)findViewById(R.id.editresult);
				clipboard.setText(edit.getText().toString());
				edit.setText("");
				findViewById(R.id.copy).setEnabled(false);
				finish();
			}
		});

		onClickListener.onClick(null);

		SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		onSharedPreferenceChanged(prefs, PREF_SHOWNAVIGATION);

		// If there's no pref for stats-reporting, set it on
		if(!prefs.contains(PREF_REPORTSTATS))
		{
			prefs.edit().putBoolean(PREF_REPORTSTATS, true).commit();
		}
	}

	@Override
	protected void onDestroy()
	{
		PreferenceManager.getDefaultSharedPreferences(this).
			unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	@Override
	protected void quit()
	{
		Intent serviceIntent = new Intent(this, IconService.class);
		stopService(serviceIntent);
		super.quit();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
		String key)
	{
		if(key.equals(PREF_SHOWNAVIGATION))
		{
			boolean show = sharedPreferences.getBoolean(PREF_SHOWNAVIGATION, false);
			Intent serviceIntent = new Intent(this, IconService.class);
			if(show)
			{
				startService(serviceIntent);
			}
			else
			{
				stopService(serviceIntent);
			}
		}
		else if(key.equals(PREF_STARTWITHSYSTEM))
		{
			int flag = sharedPreferences.getBoolean(PREF_STARTWITHSYSTEM, false)
				? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
				: PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
			ComponentName component = new ComponentName(this, StartupReceiver.class);
			getPackageManager().setComponentEnabledSetting(component, flag,
				PackageManager.DONT_KILL_APP);
		}
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
			String kanji = data.getStringExtra(PickKanjiActivity.EXTRA_KANJI);
			EditText editText = (EditText)findViewById(R.id.editresult);
			editText.setText(editText.getText() + kanji);
			findViewById(R.id.copy).setEnabled(true);
		}
	}
}