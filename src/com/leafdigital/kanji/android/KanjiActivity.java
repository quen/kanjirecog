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

import android.app.Activity;
import android.content.Intent;
import android.view.*;

public abstract class KanjiActivity extends Activity
{
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
		case R.id.settings:
			startActivity(new Intent(this, KanjiPreferenceActivity.class));
			return true;
		case R.id.quit:
			quit();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Called when activity is being 'quit'.
	 */
	protected void quit()
	{
		Intent intent = new Intent("QUIT");
		setResult(RESULT_OK, intent);
		finish();
	}

	/**
	 * Helper function so that QUIT can be propogated.
	 * @param intent Intent from onActivityResult.
	 * @return True if app is being quit
	 */
	protected boolean checkQuit(Intent intent)
	{
		if(intent != null && intent.getAction() != null
			&& intent.getAction().equals("QUIT"))
		{
			quit();
			return true;
		}
		else
		{
			return false;
		}
	}
}