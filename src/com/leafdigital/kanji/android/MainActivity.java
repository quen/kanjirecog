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
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;

public class MainActivity extends Activity
{
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
			}
		});

		onClickListener.onClick(null);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(resultCode == RESULT_OK)
		{
			String kanji = data.getStringExtra(PickKanjiActivity.EXTRA_KANJI);
			EditText editText = (EditText)findViewById(R.id.editresult);
			editText.setText(editText.getText() + kanji);
			findViewById(R.id.copy).setEnabled(true);
		}
	}
}