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

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.format.Time;

public class Timer
{
	public long id = -1;
	public String name = "";
	public boolean enabled = false;
	public long nextMillis = 0,
			intervalSecs = 4*60*60,
			nightStart=0,
			nightStop=8*60*60;
	public Uri dayTone = null, nightTone = null;
	public boolean nightNext = false,
			dayLED = true, dayWait = true,
			nightLED = false, nightWait = true;

	public Timer()
	{
		dayTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
	}

	protected void setNextAlarm(Context context)
	{
		AlarmManager alarms = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(Receiver.ACTION_ALARM);
		i.putExtra(TimerDB.KEY_ID, id);
		PendingIntent p = PendingIntent.getBroadcast(context, 0, i, 0);
		if (enabled) {
			alarms.setRepeating(AlarmManager.RTC_WAKEUP, nextMillis, 600000, p);  // 10 minutes
		} else {
			alarms.cancel(p);
		}
	}
	
	protected void reset()
	{
		nextMillis = (enabled ? System.currentTimeMillis() : 0) + intervalSecs*1000 + 3;
	}
	
	protected static long timeOfDaySecs()
	{
		Time t = new Time();
		t.setToNow();
		return t.hour*3600 + t.minute*60 + t.second;
	}
	
	protected boolean isNight()
	{
		// TODO: don't wake me up: check for any overlap between (nextMillis,now) and (nightStart,nightStop)
		if (nightNext) return true;
		long timeOfDay = timeOfDaySecs();
		boolean midnightIsNight = nightStop < nightStart,
				postStart = timeOfDay >= nightStart,
				preEnd = timeOfDay < nightStop;
		return midnightIsNight ? (postStart || preEnd) : (postStart && preEnd);
	}
	
	protected boolean shouldWait()
	{
		return isNight() ? nightWait : dayWait;
	}
	
	protected void unNotify(Context context)
	{
		NotificationManager notifications = (NotificationManager)
				context.getSystemService(Context.NOTIFICATION_SERVICE);
		notifications.cancel((int)id);
	}
	
	protected void notify(Context context)
	{
		if (! enabled) {
			unNotify(context);
			return;
		}
		NotificationManager notifications = (NotificationManager)
				context.getSystemService(Context.NOTIFICATION_SERVICE);
		boolean isNight = isNight();
		nightNext = false;
		boolean useLED = isNight ? nightLED : dayLED; 
		String text = name.length() > 0 ? name : "Timer"; 
		Notification n = new Notification(R.drawable.icon, text,
				nextMillis);
		n.setLatestEventInfo(context, text, null, PendingIntent.getActivity(
				context, 0, new Intent(context, TimerActivity.class)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0));
		n.flags = Notification.FLAG_NO_CLEAR
				| (useLED ? Notification.FLAG_SHOW_LIGHTS : 0);
		if (useLED) {
			n.ledOnMS = 250;
			n.ledOffMS = 1250;
			n.ledARGB = 0xff2222ff;
		}
		n.audioStreamType = AudioManager.STREAM_ALARM;
		n.sound = isNight ? nightTone : dayTone;
		notifications.notify((int)id, n);
	}
}
