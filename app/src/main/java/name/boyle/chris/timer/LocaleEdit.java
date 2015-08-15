package name.boyle.chris.timer;

import java.text.MessageFormat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.twofortyfouram.SharedResources;

/**
 * This is the "Edit" activity for a <i>Locale</i> plug-in.
 */
public final class LocaleEdit extends Activity
{

	/**
	 * Menu ID of the save item.
	 */
	private static final int MENU_SAVE = 1;

	/**
	 * Menu ID of the don't save item.
	 */
	private static final int MENU_DONT_SAVE = 2;

	static final String BUNDLE_EXTRA_MINS = "name.boyle.chris.timer.EXTRA_MINS";
	static final String BUNDLE_EXTRA_ID = "name.boyle.chris.timer.EXTRA_ID";

	/**
	 * Flag boolean that can only be set to true via the "Don't Save" menu item in {@link #onMenuItemSelected(int, MenuItem)}. If
	 * true, then this {@code Activity} should return {@link Activity#RESULT_CANCELED} in {@link #finish()}.
	 * <p>
	 * There is no need to save/restore this field's state when the {@code Activity} is paused.
	 */
	private boolean isCancelled;

	private TimerDB db;
	private Cursor timers;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.locale_edit);

		/*
		 * Locale guarantees that the breadcrumb string will be present, but checking for null anyway makes your Activity more
		 * robust and re-usable
		 */
		final String breadcrumbString = getIntent().getStringExtra(com.twofortyfouram.Intent.EXTRA_STRING_BREADCRUMB);
		if (breadcrumbString != null)
			setTitle(String.format("%s%s%s", breadcrumbString, com.twofortyfouram.Intent.BREADCRUMB_SEPARATOR, getString(R.string.plugin_name))); //$NON-NLS-1$

		/*
		 * Load the Locale background frame from Locale
		 */
		((LinearLayout) findViewById(R.id.frame)).setBackgroundDrawable(SharedResources.getDrawableResource(getPackageManager(), SharedResources.DRAWABLE_LOCALE_BORDER));

		/*
		 * populate the picker
		 */
		final NumberPicker picker = ((NumberPicker) findViewById(R.id.picker));
		picker.setRange(0, 995);
		picker.setStep(5);

		db = new TimerDB(this);
		db.open();
		timers = db.getAllEntries();

		final Spinner spinner = ((Spinner) findViewById(R.id.spinner));
		SimpleCursorAdapter a = new SimpleCursorAdapter(this, android.R.layout.simple_spinner_item,
				timers, new String[]{TimerDB.KEY_NAME}, new int[]{android.R.id.text1});
		a.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
			public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
				String name = cursor.getString(columnIndex);
				if (name == null || name.length() == 0) {
					name = getString(R.string.locale_spinner_no_name);
				}
				((TextView)view).setText(MessageFormat.format(getString(R.string.locale_spinner_text),
						cursor.getPosition()+1, name));
				return true;
			}
		});
		a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(a);

		/*
		 * if savedInstanceState == null, then we are entering the Activity directly from Locale and we need to check whether the
		 * Intent has forwarded a Bundle extra (e.g. whether we editing an old condition or creating a new one)
		 */
		if (savedInstanceState == null)
		{
			final Bundle forwardedBundle = getIntent().getBundleExtra(com.twofortyfouram.Intent.EXTRA_BUNDLE);

			/*
			 * the forwardedBundle would be null if this was a new condition
			 */
			if (forwardedBundle != null)
			{
				picker.setCurrent(forwardedBundle.getInt(BUNDLE_EXTRA_MINS, 0));
				long id = forwardedBundle.getLong(BUNDLE_EXTRA_ID, -1);
				if (id >= 0 && timers.moveToFirst()) {
					do {
						if (timers.getLong(TimerDB.COL_ID) == id) {
							spinner.setSelection(timers.getPosition());
							break;
						}
					} while (timers.moveToNext());
				}
			}
		}
		/*
		 * if savedInstanceState != null, there is no need to restore any Activity state directly (e.g. onSaveInstanceState()).
		 * This is handled by the Spinner automatically
		 */
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		timers.close();
		db.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void finish()
	{
		if (isCancelled)
			setResult(RESULT_CANCELED);
		else
		{
			final NumberPicker picker = ((NumberPicker) findViewById(R.id.picker));
			final Spinner spinner = ((Spinner) findViewById(R.id.spinner));

			/*
			 * This is the return Intent, into which we'll put all the required extras
			 */
			final Intent returnIntent = new Intent();

			/*
			 * This extra is the data to ourselves: either for the Activity or the BroadcastReceiver. Note that anything placed in
			 * this Bundle must be available to Locale's class loader. So storing String, int, and other basic objects will work
			 * just fine. You cannot store an object that only exists in your app, as Locale will be unable to serialize it.
			 */
			final Bundle storeAndForwardExtras = new Bundle();

			int mins = picker.getCurrent();
			long id = spinner.getSelectedItemId();
			storeAndForwardExtras.putInt(BUNDLE_EXTRA_MINS, mins);
			storeAndForwardExtras.putLong(BUNDLE_EXTRA_ID, id);
			returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BLURB,
					MessageFormat.format(getString(R.string.n_mins), mins, id));
			returnIntent.putExtra(com.twofortyfouram.Intent.EXTRA_BUNDLE, storeAndForwardExtras);

			setResult(RESULT_OK, returnIntent);
		}

		super.finish();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu)
	{
		super.onCreateOptionsMenu(menu);

		final PackageManager manager = getPackageManager();

		final Intent helpIntent = new Intent(com.twofortyfouram.Intent.ACTION_HELP);
		helpIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_HELP_URL, getString(R.string.docs_url));

		// Note: title set in onCreate
		helpIntent.putExtra(com.twofortyfouram.Intent.EXTRA_STRING_BREADCRUMB, getTitle());

		/*
		 * We are dynamically loading resources from Locale's APK. This will only work if Locale is actually installed
		 */
		menu.add(SharedResources.getTextResource(manager, SharedResources.STRING_MENU_HELP))
			.setIcon(SharedResources.getDrawableResource(manager, SharedResources.DRAWABLE_MENU_HELP)).setIntent(helpIntent);

		menu.add(0, MENU_DONT_SAVE, 0, SharedResources.getTextResource(manager, SharedResources.STRING_MENU_DONTSAVE))
			.setIcon(SharedResources.getDrawableResource(manager, SharedResources.DRAWABLE_MENU_DONTSAVE)).getItemId();

		menu.add(0, MENU_SAVE, 0, SharedResources.getTextResource(manager, SharedResources.STRING_MENU_SAVE))
			.setIcon(SharedResources.getDrawableResource(manager, SharedResources.DRAWABLE_MENU_SAVE)).getItemId();

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onMenuItemSelected(final int featureId, final MenuItem item)
	{
		switch (item.getItemId())
		{
			case MENU_SAVE:
			{
				finish();
				return true;
			}
			case MENU_DONT_SAVE:
			{
				isCancelled = true;
				finish();
				return true;
			}
		}

		return super.onOptionsItemSelected(item);
	}
}
