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

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

public class HMSPicker extends LinearLayout
{
	public interface OnChangedListener {
        void onChanged(HMSPicker picker, int oldVal, int newVal);
    }

	private OnChangedListener mListener;
	NumberPicker hourPicker, minPicker, secPicker;
	protected int mPrevious;
	protected NumberPicker.OnChangedListener passTheBuck = new NumberPicker.OnChangedListener() {
		public void onChanged(NumberPicker picker, int oldVal, int newVal) {
			notifyChange();
		}
    };

	public void setOnChangeListener(OnChangedListener listener) {
        mListener = listener;
    }

	public HMSPicker(Context context)
	{
		this(context, null);
	}

	public HMSPicker(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setOrientation(HORIZONTAL);
		LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.hms_picker, this, true);
        try {
	        hourPicker = (NumberPicker)findViewById(R.id.h);
	        hourPicker.setRange(0,99);
	        hourPicker.setOnChangeListener(passTheBuck);
	        minPicker = (NumberPicker)findViewById(R.id.m);
	        hourPicker.smaller = minPicker;
	        minPicker.larger = hourPicker;
	        minPicker.setRange(0,59);
	        minPicker.setSpeed(100);
	        minPicker.setOnChangeListener(passTheBuck);
	        secPicker = (NumberPicker)findViewById(R.id.s);
	        minPicker.smaller = secPicker;
	        secPicker.larger = minPicker;
	        secPicker.setRange(0,59);
	        secPicker.setSpeed(100);
	        secPicker.setOnChangeListener(passTheBuck);
		} catch (ClassCastException issue6894) {}  // just the ADT layout editor failing for some reason
	}
	
	

	public void setSecs(int secs)
	{
		int s = secs % 60, m = (secs/60) % 60, h = Math.min(secs/3600, 99);
		hourPicker.setCurrent(h);
		minPicker.setCurrent(m);
		secPicker.setCurrent(s);
	}

	protected void notifyChange() {
		int current = getSecs();
		if (current == mPrevious) return;
        if (mListener != null) {
            mListener.onChanged(this, mPrevious, current);
        }
        mPrevious = current;
    }
	
	public int getSecs()
	{
		return hourPicker.getCurrent()*3600 + minPicker.getCurrent()*60 + secPicker.getCurrent();
	}
}
