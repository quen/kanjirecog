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

import android.app.*;
import android.content.Intent;
import android.os.*;

/**
 * Service that just displays the notification icon.
 */
public class IconService extends Service
{
	private IBinder binder = new Binder();
	private NotificationManager notifications;

	@Override
	public IBinder onBind(Intent intent)
	{
		return binder;
	}

	@Override
	public void onCreate()
	{
		notifications = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		Notification notification = new Notification(R.drawable.statusicon,
			getString(R.string.notificationtitle), 0L);
		notification.flags |=
			Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;
		PendingIntent launchIntent = PendingIntent.getActivity(this, 0,
      new Intent(this, MainActivity.class), 0);
		notification.setLatestEventInfo(this, getString(R.string.notificationtitle),
			getString(R.string.notificationtext), launchIntent);

		notifications.notify(1, notification);
	}

	public void onDestroy()
	{
		notifications.cancel(1);
	}
}
