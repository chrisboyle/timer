package name.boyle.chris.timer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.widget.Toast;
import android.widget.ToggleButton;

public class LongPressToggle extends ToggleButton {
	public LongPressToggle(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		setLongClickable(true);
	}

	@Override
	public boolean performClick()
	{
		Toast t = Toast.makeText(getContext(),
				"Toggle requires a long press",
				Toast.LENGTH_LONG);
		t.setGravity(Gravity.TOP, 0, 20);
		t.show();
		return false;
	}
	
	@Override
	public boolean performLongClick()
	{
		toggle();
		performHapticFeedback(
				HapticFeedbackConstants.LONG_PRESS,
				HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
		return true;
	}
}
