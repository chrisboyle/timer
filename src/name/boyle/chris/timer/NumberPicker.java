/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NumberPicker extends LinearLayout implements OnClickListener,
        OnLongClickListener {

    public interface OnChangedListener {
        void onChanged(NumberPicker picker, int oldVal, int newVal);
    }

    public interface Formatter {
        String toString(int value);
    }

    /*
     * Use a custom NumberPicker formatting callback to use two-digit
     * minutes strings like "01".  Keeping a static formatter etc. is the
     * most efficient way to do this; it avoids creating temporary objects
     * on every call to format().
     */
    public static final NumberPicker.Formatter TWO_DIGIT_FORMATTER =
            new NumberPicker.Formatter() {
                final StringBuilder mBuilder = new StringBuilder();
                final java.util.Formatter mFmt = new java.util.Formatter(mBuilder);
                final Object[] mArgs = new Object[1];
                public String toString(int value) {
                    mArgs[0] = value;
                    mBuilder.delete(0, mBuilder.length());
                    mFmt.format("%02d", mArgs);
                    return mFmt.toString();
                }
        };

    private final Handler mHandler;
    private final Runnable mRunnable = new Runnable() {
        public void run() {
            if (mIncrement) {
                increment();
                mHandler.postDelayed(this, mSpeed);
            } else if (mDecrement) {
                decrement();
                mHandler.postDelayed(this, mSpeed);
            }
        }
    };

    private final TextView mText;

    protected int mStart;
    protected int mEnd;
    protected int mCurrent;
    protected int mPrevious;
    private OnChangedListener mListener;
    private Formatter mFormatter;
    private long mSpeed = 300;
    protected NumberPicker smaller, larger;

    private boolean mIncrement;
    private boolean mDecrement;

    public NumberPicker(Context context) {
        this(context, null);
    }

    public NumberPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs);
        setOrientation(VERTICAL);
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.number_picker, this, true);
        mHandler = new Handler();
        try {
        mIncrementButton = (NumberPickerButton) findViewById(R.id.increment);
        mIncrementButton.setOnClickListener(this);
        mIncrementButton.setOnLongClickListener(this);
        mIncrementButton.setNumberPicker(this);
        mDecrementButton = (NumberPickerButton) findViewById(R.id.decrement);
        mDecrementButton.setOnClickListener(this);
        mDecrementButton.setOnLongClickListener(this);
        mDecrementButton.setNumberPicker(this);
        } catch (ClassCastException issue6894) {}  // just the ADT layout editor failing for some reason

        mText = (TextView) findViewById(R.id.timepicker_input);

        if (!isEnabled()) {
            setEnabled(false);
        }
        setFormatter(TWO_DIGIT_FORMATTER);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mIncrementButton.setEnabled(enabled);
        mDecrementButton.setEnabled(enabled);
        mText.setEnabled(enabled);
    }

    public void setOnChangeListener(OnChangedListener listener) {
        mListener = listener;
    }

    public void setFormatter(Formatter formatter) {
        mFormatter = formatter;
    }

    /**
     * Set the range of numbers allowed for the number picker. The current
     * value will be automatically set to the start.
     *
     * @param start the start of the range (inclusive)
     * @param end the end of the range (inclusive)
     */
    public void setRange(int start, int end) {
        mStart = start;
        mEnd = end;
        mCurrent = start;
        updateView();
    }

    public void setCurrent(int current) {
        mCurrent = current;
        updateView();
    }

    /**
     * The speed (in milliseconds) at which the numbers will scroll
     * when the the +/- buttons are longpressed. Default is 300ms.
     */
    public void setSpeed(long speed) {
        mSpeed = speed;
    }

    public void onClick(View v) {
        // now perform the increment/decrement
        if (R.id.increment == v.getId()) {
        	increment();
        } else if (R.id.decrement == v.getId()) {
            decrement();
        }
    }
    
    void increment()
    {
    	if (mCurrent == mEnd) {
    		boolean ok = false;
    		NumberPicker np = larger;
    		while (np!=null) {
    			if (np.mCurrent < mEnd) { ok = true; break; }
    			np = np.larger;
    		}
    		if (!ok) {
    			np = smaller;
    			while (np!=null) {
    				np.changeCurrent(np.mEnd);
    				np = np.smaller;
    			}
    			return;
    		}
    		larger.increment();
    	}
        changeCurrent(mCurrent + 1);    
    }
    
    void decrement()
    {
    	if (mCurrent == mStart) {
    		boolean ok = false;
    		NumberPicker np = larger;
    		while (np!=null) {
    			if (np.mCurrent > mStart) { ok = true; break; }
    			np = np.larger;
    		}
    		if (!ok) {
    			np = smaller;
    			while (np!=null) {
    				np.changeCurrent(np.mStart);
    				np = np.smaller;
    			}
    			return;
    		}
    		larger.decrement();
    	}
        changeCurrent(mCurrent - 1);    
    }

    private String formatNumber(int value) {
        return (mFormatter != null)
                ? mFormatter.toString(value)
                : String.valueOf(value);
    }

    protected void changeCurrent(int current) {
        // Wrap around the values if we go past the start or end
        if (current > mEnd) {
            current = mStart;
        } else if (current < mStart) {
            current = mEnd;
        }
        mPrevious = mCurrent;
        mCurrent = current;
        notifyChange();
        updateView();
    }

    protected void notifyChange() {
        if (mListener != null) {
            mListener.onChanged(this, mPrevious, mCurrent);
        }
    }

    protected void updateView() {
        mText.setText(formatNumber(mCurrent));
    }

    /**
     * We start the long click here but rely on the {@link NumberPickerButton}
     * to inform us when the long click has ended.
     */
    public boolean onLongClick(View v) {

        /* The text view may still have focus so clear it's focus which will
         * trigger the on focus changed and any typed values to be pulled.
         */
        mText.clearFocus();

        if (R.id.increment == v.getId()) {
            mIncrement = true;
            mHandler.post(mRunnable);
        } else if (R.id.decrement == v.getId()) {
            mDecrement = true;
            mHandler.post(mRunnable);
        }
        return true;
    }

    public void cancelIncrement() {
        mIncrement = false;
    }

    public void cancelDecrement() {
        mDecrement = false;
    }

    private NumberPickerButton mIncrementButton;
    private NumberPickerButton mDecrementButton;

    /**
     * @return the current value.
     */
    public int getCurrent() {
        return mCurrent;
    }
}