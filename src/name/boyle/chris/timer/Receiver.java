/*
 * Copyright (C) 2010 Chris Boyle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package name.boyle.chris.timer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

public class Receiver extends BroadcastReceiver
{
	protected static final String ACTION_ALARM = "name.boyle.chris.timer.ALARM";

	@Override
	public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_BOOT_COMPLETED) || action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
			// Alarms don't survive reboots or upgrades, so restore them all
			TimerDB db = new TimerDB(context);
			db.open();
			Cursor c = db.getAllEntries();
			if (c.moveToFirst()) {
				do {
					Timer t = db.cursorToEntry(c);
					if (! t.enabled) continue;
					t.setNextAlarm(context);
				} while (c.moveToNext());
			}
			c.close();
			db.close();
		} else if (action.equals(ACTION_ALARM)) {
			// It's time to sound/show an alarm
			Log.d(TimerActivity.TAG, "ACTION_ALARM: \""+intent.getData().toString()+"\"");
			final long id;
			try {
				id = Long.parseLong(intent.getData().getSchemeSpecificPart());
			} catch (NumberFormatException e) {
				Log.e(TimerActivity.TAG, "NumberFormatException! "+e.toString());
				return;
			}
			// We ask TimerActivity to do this (rather than say we've done it)
			// to avoid conflicting modifications in the case where a delayed
			// save has already been queued
			Intent i = new Intent(TimerActivity.ACTION_RESET);
			i.putExtra(TimerDB.KEY_ID, id);
			Log.d(TimerActivity.TAG, "Pinging TimerActivity...");
			context.sendOrderedBroadcast(i, null, new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					int result = getResultCode();
					if (result != Activity.RESULT_CANCELED) {
						Log.d(TimerActivity.TAG, "TimerActivity caught the broadcast, result "+result);
						return;  // Activity caught it
					}
					Log.d(TimerActivity.TAG, "TimerActivity did not catch the broadcast");
					TimerDB db = new TimerDB(context);
					db.open();
					Timer t = db.getEntry(id);
					if (t.notify(context)) db.saveEntry(t);
					db.close();
				}
			}, null, Activity.RESULT_CANCELED, null, null);
			Timer.requeryLocale(context);
		} else if (action.equals(com.twofortyfouram.Intent.ACTION_QUERY_CONDITION)) {
			final Bundle bundle = intent.getBundleExtra(com.twofortyfouram.Intent.EXTRA_BUNDLE);
			if (bundle == null)
			{
				Log.e(TimerActivity.TAG, "Received null BUNDLE"); //$NON-NLS-1$
				return;
			}
			if (!bundle.containsKey(LocaleEdit.BUNDLE_EXTRA_MINS) || !bundle.containsKey(LocaleEdit.BUNDLE_EXTRA_ID))
			{
				Log.e(TimerActivity.TAG, "Missing param in Bundle"); //$NON-NLS-1$
				return;
			}
			int mins = bundle.getInt(LocaleEdit.BUNDLE_EXTRA_MINS);
			long id = bundle.getLong(LocaleEdit.BUNDLE_EXTRA_ID);
			TimerDB db = new TimerDB(context);
			db.open();
			Timer t = db.getEntry(id);
			db.close();
			if (t == null) {
				setResultCode(com.twofortyfouram.Intent.RESULT_CONDITION_UNKNOWN);
			} else if (t.isLateByMins(mins)) {
				setResultCode(com.twofortyfouram.Intent.RESULT_CONDITION_SATISFIED);
			} else {
				setResultCode(com.twofortyfouram.Intent.RESULT_CONDITION_UNSATISFIED);
			}
		}
	}
}
