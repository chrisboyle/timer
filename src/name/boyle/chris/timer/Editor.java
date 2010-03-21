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

import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.ToggleButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Editor extends RelativeLayout
{
	static final String NO_TIME = "--:--";
	EditText timerName;
	ToggleButton toggler;
    HMSPicker next, interval;
	Button reset, nightStart, nightStop, dayTone, nightTone;
	CheckBox dayLED, dayWait, nightLED, nightWait, nightNext;
	TextView nextAlarm, nextAlarm2;
	Timer timer;
	TimerActivity parent;
	private boolean ignoreChanges;

	public Editor(Context context)
	{
		this(context, null);
	}

	public Editor(final Context context, AttributeSet attrs)
	{
		super(context, attrs);
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.editor, this, true);
        reset = (Button)findViewById(R.id.reset);
        reset.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				timer.reset();
				parent.save();
				timer.unNotify(context);
				timer.setNextAlarm(context);
				parent.ticker.run();
			}
		});
        timerName = (EditText)findViewById(R.id.timerName);
        timerName.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {
				if (ignoreChanges) return;
				timer.name = s.toString();
				parent.delayedSave();
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
        toggler = (ToggleButton)findViewById(R.id.toggler);
        toggler.setOnCheckedChangeListener(new ToggleButton.OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (ignoreChanges) return;
				timer.enabled = isChecked;
				int secs = next.getSecs();
				if (secs <= 0) secs += interval.getSecs();
				timer.nextMillis = secs*1000 + (isChecked ? System.currentTimeMillis()+3 : 0);
				parent.handler.removeCallbacks(parent.ticker);
				if (isChecked) parent.handler.postDelayed(parent.ticker, 1003);
				parent.save();
				timer.setNextAlarm(getContext());
				if (!isChecked) timer.notify(getContext());
			}
        });
        if (! isInEditMode()) {
			next = (HMSPicker)findViewById(R.id.next);
			next.setOnChangeListener(new HMSPicker.OnChangedListener() {
				public void onChanged(HMSPicker picker, int oldVal, int newVal) {
					if (ignoreChanges) return;
					int s = next.getSecs();
					if (s > 0) timer.unNotify(context);
					timer.nextMillis = s*1000 + (timer.enabled ? System.currentTimeMillis()-3 : 0);
					if (timer.enabled) parent.ticker.run(); else updateNextTimes();
					timer.setNextAlarm(getContext());
					parent.delayedSave();
				}
			});
	        interval = (HMSPicker)findViewById(R.id.interval);
	        interval.setOnChangeListener(new HMSPicker.OnChangedListener() {
				public void onChanged(HMSPicker picker, int oldVal, int newVal) {
					if (ignoreChanges) return;
					timer.intervalSecs = interval.getSecs();
					if (timer.nextMillis > System.currentTimeMillis())
						timer.setNextAlarm(getContext());
					updateNextTimes();
					parent.delayedSave();
				}
			});
        }
        nextAlarm = (TextView)findViewById(R.id.nextAlarm);
        nextAlarm.setText(NO_TIME);
        nextAlarm2 = (TextView)findViewById(R.id.nextAlarm2);
        nextAlarm2.setText(NO_TIME);
        nightStart = (Button)findViewById(R.id.nightStart);
        nightStop = (Button)findViewById(R.id.nightStop);
        nightNext = (CheckBox)findViewById(R.id.nightNext);
        nightNext.setOnCheckedChangeListener(cbChanged);
        dayTone = (Button)findViewById(R.id.dayTone);
        dayTone.setOnClickListener(pickTone);
        dayLED = (CheckBox)findViewById(R.id.dayLED);
        dayLED.setOnCheckedChangeListener(cbChanged);
        dayWait = (CheckBox)findViewById(R.id.dayWait);
        dayWait.setOnCheckedChangeListener(cbChanged);
        nightTone = (Button)findViewById(R.id.nightTone);
        nightTone.setOnClickListener(pickTone);
        nightLED = (CheckBox)findViewById(R.id.nightLED);
        nightLED.setOnCheckedChangeListener(cbChanged);
        nightWait = (CheckBox)findViewById(R.id.nightWait);
        nightWait.setOnCheckedChangeListener(cbChanged);
        if (! isInEditMode()) {
	    	OnClickListener setTime = new OnClickListener() {
	    		public void onClick(View v) {
	    			final boolean isStop = v == nightStop;
	    			int t = isStop ? (int)timer.nightStop : (int)timer.nightStart,
	    					h = t / 3600, m = (t%3600)/60;
	    			new TimePickerDialog(getContext(), new OnTimeSetListener() {
	    				public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
	    					int result = hourOfDay*3600+minute*60;
	    					if (isStop) timer.nightStop = result; else timer.nightStart = result;
	    					Button target = isStop ? nightStop : nightStart;
	    					target.setText(secsToHHMM(isStop ? timer.nightStop : timer.nightStart));
	    					parent.save();
	    				}
	    			}, h, m, DateFormat.is24HourFormat(getContext())).show();
	    		}
	    	};
	        nightStart.setOnClickListener(setTime);
	        nightStop.setOnClickListener(setTime);
        }
	}
	
	protected void updateNextTimes()
	{
		Time t = new Time();
		long ms = (timer.enabled ? 0 : System.currentTimeMillis()) + timer.nextMillis;
		t.set(ms);
		nextAlarm.setText(t.format("%H:%M"));
		if (timer.intervalSecs > 0) {
			t.set(ms+timer.intervalSecs*1000);
			nextAlarm2.setText(t.format("%H:%M"));
		} else {
			nextAlarm2.setText(NO_TIME);
		}
	}
		
	OnCheckedChangeListener cbChanged = new OnCheckedChangeListener() {
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (ignoreChanges) return;
			switch (buttonView.getId()) {
			case R.id.nightNext: timer.nightNext = isChecked; break;
			case R.id.dayLED: timer.dayLED = isChecked; break;
			case R.id.dayWait: timer.dayWait = isChecked; break;
			case R.id.nightLED: timer.nightLED = isChecked; break;
			case R.id.nightWait: timer.nightWait = isChecked; break;
			}
			parent.save();
		}
	};

    Button.OnClickListener pickTone = new Button.OnClickListener() { public void onClick(View v) {
    	boolean isNight = v == nightTone;
    	Intent i = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
    	i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, isNight ? timer.nightTone : timer.dayTone);
    	i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
    	parent.startActivityForResult(i, isNight ? TimerActivity.NIGHT_TONE : TimerActivity.DAY_TONE);
	}};
    
    protected void setUIFromTimer()
    {
    	ignoreChanges = true;
    	timerName.setText(timer.name);
    	toggler.setChecked(timer.enabled);
    	setNextPicker();
    	interval.setSecs((int)timer.intervalSecs);
    	updateNextTimes();
        updateTone(false);
        dayLED.setChecked(timer.dayLED);
        dayWait.setChecked(timer.dayWait);
        updateTone(true);
        nightLED.setChecked(timer.nightLED);
        nightWait.setChecked(timer.nightWait);
        nightStart.setText(secsToHHMM(timer.nightStart));
        nightStop.setText(secsToHHMM(timer.nightStop));
        nightNext.setChecked(timer.nightNext);
        ignoreChanges = false;
    }
    
    protected void setTone(boolean isNight, Uri uri)
    {
    	if (isNight) timer.nightTone = uri; else timer.dayTone = uri;
    	updateTone(isNight);
    	parent.save();
    }

    protected void updateTone(boolean isNight)
    {
    	Uri uri = isNight ? timer.nightTone : timer.dayTone;
    	Button target = isNight ? nightTone : dayTone;
        if (uri == null) {
        	target.setText("Silent");
        } else if (RingtoneManager.isDefault(uri)) {
    		target.setText("Default");
    	} else {
    		Ringtone r = RingtoneManager.getRingtone(getContext(), uri);
   			target.setText(r.getTitle(getContext()));
    	}
    }
    
    protected String secsToHHMM(long secs)
    {
    	return String.format("%02d:%02d", secs/3600, (secs%3600)/60);
    }
    
    protected void setNextPicker()
    {
    	if (timer.enabled) {
    		long in = timer.nextMillis - System.currentTimeMillis();
    		long out = Math.round(in/1000.0);
    		Log.d(TimerActivity.TAG, "setNextPicker "+timer.nextMillis+", "+in+", "+out);
    		next.setSecs((int)Math.max(0, out));
    	} else {
    		next.setSecs((int)timer.nextMillis/1000);
    	}
    }
}
