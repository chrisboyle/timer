/*
 * Copyright (C) 2010 Chris Boyle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package name.boyle.chris.timer;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewSwitcher;

public class TimerActivity extends Activity
{
	public static final String TAG = "Timer";
	public static final int DAY_TONE = 1, NIGHT_TONE = 2;
	static final String ACTION_RESET = "name.boyle.chris.timer.RESET";
	RingtoneManager rtm;
	TimerDB db;
	ViewSwitcher switcher;
	ImageButton prevBtn, nextBtn;
	Cursor timers;
	TextView positionText;
	Handler handler = new Handler();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		switcher = (ViewSwitcher)findViewById(R.id.switcher);
		positionText = (TextView)findViewById(R.id.positionText);
		prevBtn = (ImageButton)findViewById(R.id.prevBtn);
		prevBtn.setOnClickListener(prevNextListener);
		nextBtn = (ImageButton)findViewById(R.id.nextBtn);
		nextBtn.setOnClickListener(prevNextListener);
		Editor e = (Editor)findViewById(R.id.editor2);
		e.parent = this;
		e.timer = new Timer();  // just so restoreInstanceState doesn't crash
		e = (Editor)findViewById(R.id.editor1);
		e.parent = this;

		db = new TimerDB(this);
		db.open();
		timers = db.getAllEntries();
		long id = idFromIntent(getIntent());
		e.timer = db.getEntry(id);
		if (e.timer != null) {
			seenTimer(e.timer);
		} else if (timers.moveToFirst()) {
			e.timer = db.cursorToEntry(timers);
		} else {
			e.timer = new Timer();
			delayedSave();
		}
		e.setUIFromTimer();
		updatePosition();
		if (e.timer.nextMillis > System.currentTimeMillis()) e.timer.setNextAlarm(this);
	}

	@Override
	public void onNewIntent(Intent i)
	{
		long id = idFromIntent(i);
		Timer t = ((Editor)switcher.getCurrentView()).timer;
		long currentId = t.id;
		if (currentId != id) {
			t = db.getEntry(id);
			if (t == null) return;
			if (needSave) save();
			TimerDB.moveCursorTo(timers, id);
			updatePosition();
			Editor newEd = (Editor)switcher.getNextView();
			newEd.timer = t;
			newEd.setUIFromTimer();
			flip(t.id > currentId);
			ticker.run();
		}
		seenTimer(t);
	}

	private void seenTimer(Timer t)
	{
		// Clicked on notification, so Locale should not trigger attention-grabbing
		if (t.seen) return;
		t.seen = true;
		save();
		Timer.requeryLocale(this);
	}

	private long idFromIntent(Intent i)
	{
		Uri u = i.getData();
		if (u == null) return -1;
		try {
			return Long.parseLong(u.getSchemeSpecificPart());
		} catch (NumberFormatException e) {
			Log.e(TimerActivity.TAG, "NumberFormatException! "+e.toString());
			return -1;
		}
	}

	public BroadcastReceiver resetReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			long id = intent.getLongExtra(TimerDB.KEY_ID, -1);
			final Editor ed = (Editor)switcher.getCurrentView();
			if (ed.timer.id != id) return;
			if (ed.timer.notify(context)) {
				save();
				runOnUiThread(new Runnable() {
					public void run() {
						ed.setUIFromTimer();
					}
				});
			}
			this.setResultCode(Activity.RESULT_OK);
		}
	};

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		timers.close();
		db.close();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		handler.removeCallbacks(ticker);
		unregisterReceiver(resetReceiver);
		if (needSave) save();
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		registerReceiver(resetReceiver, new IntentFilter(ACTION_RESET));
		ticker.run();
	}

	protected Runnable ticker = new Runnable() {
		public void run() {
			handler.removeCallbacks(this);
			Editor e = (Editor)switcher.getCurrentView();
			Timer t = e.timer;
			if (! t.enabled) return;
			e.setNextPicker();
			e.updateNextTimes();
			long remaining = t.nextMillis - System.currentTimeMillis();
			handler.postDelayed(this, (remaining > 0) ? (remaining%1000 + 3) : 500);
		}
	};

	protected void save()
	{
		handler.removeCallbacks(delayedSaver);
		saveWithoutRequery();
		int p = timers.getPosition();
		if (p < 0) p = 0;
		timers.requery();
		if (! timers.moveToPosition(p)) throw new RuntimeException("cursor disappeared!");
	}

	protected void saveWithoutRequery()
	{
		Timer t = ((Editor)switcher.getCurrentView()).timer;
		Log.d(TAG, "save "+t.id);
		db.saveEntry(t);
		needSave = false;
	}

	private Runnable delayedSaver = new Runnable() {
		public void run() {	save(); }
	};

	private boolean needSave = false;
	protected void delayedSave()
	{
		Log.d(TAG, "delayedSave");
		needSave = true;
		handler.removeCallbacks(delayedSaver);
		handler.postDelayed(delayedSaver, 1000);
	}

	protected void flip(boolean right)
	{
		switcher.setInAnimation(this, right ? R.anim.slide_in_right : R.anim.slide_in_left);
		switcher.setOutAnimation(this, right ? R.anim.slide_out_left : R.anim.slide_out_right);
		switcher.showNext();
	}

	ImageButton.OnClickListener prevNextListener = new ImageButton.OnClickListener() {
		public void onClick(View v) {
			if (needSave) save();
			boolean isNext = v == nextBtn;
			boolean ok = isNext ? timers.moveToNext() : timers.moveToPrevious();
			updatePosition();
			if (!ok) return;
			Editor newEd = (Editor)switcher.getNextView();
			newEd.timer = db.cursorToEntry(timers);
			newEd.setUIFromTimer();
			flip(isNext);
			ticker.run();
		}
	};

	protected void addTimer()
	{
		if (needSave) save();
		Editor newEd = (Editor)switcher.getNextView();
		newEd.timer = new Timer();
		newEd.setUIFromTimer();
		db.saveEntry(newEd.timer);
		long id = newEd.timer.id;
		timers.requery();
		timers.moveToFirst();
		while (timers.getLong(TimerDB.COL_ID) != id && timers.moveToNext()) {}
		updatePosition();
		flip(true);
		ticker.run();
	}

	protected void removeTimer()
	{
		handler.removeCallbacks(delayedSaver);
		Editor e = (Editor)switcher.getCurrentView();
		e.timer.enabled = false;
		e.timer.setNextAlarm(this);
		e.timer.notify(this);
		boolean right = timers.getPosition() == 0;
		db.removeEntry(timers.getLong(TimerDB.COL_ID));
		timers.requery();
		timers.moveToFirst();
		updatePosition();
		Editor newEd = (Editor)switcher.getNextView();
		newEd.timer = db.cursorToEntry(timers);
		newEd.setUIFromTimer();
		flip(right);
		ticker.run();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if ((requestCode != DAY_TONE && requestCode != NIGHT_TONE)
				|| resultCode != RESULT_OK) return;
		boolean isNight = requestCode == NIGHT_TONE;
		Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
		((Editor)switcher.getCurrentView()).setTone(isNight, uri);
	}

	protected void updatePosition()
	{
		int p = timers.getPosition(), c = timers.getCount();
		positionText.setText(String.format("%d/%d", p+1, c));
		prevBtn.setEnabled(p > 0);
		nextBtn.setEnabled(p < c - 1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		menu.findItem(R.id.remove).setEnabled(timers != null && timers.getCount() > 1);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
		case R.id.add: addTimer(); break;
		case R.id.remove: removeTimer(); break;
		case R.id.help:
			final Dialog d = new Dialog(this, android.R.style.Theme);
			final WebView wv = new WebView(this);
			d.setTitle("Help");
			d.setContentView(wv);
			wv.loadUrl(getString(R.string.docs_url));
			d.show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState)
	{
		// Deliberately do nothing; this app saves changes immediately.
		// Moreover, the default save and restore seems to restore from
		// a blank state, which is problematic.
	}

	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		// Do nothing, see above.
	}

}
