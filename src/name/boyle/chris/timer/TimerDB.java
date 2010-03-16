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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.net.Uri;
import android.util.Log;

public class TimerDB
{
	private static final String TAG = "TimerDB";
	private static final String DB_NAME = "timer.db";
	private static final String DB_TABLE = "timers";
	private static final int DB_VERSION = 1;
	
	public static final String KEY_ID = "_id";
	public static final int    COL_ID = 0;
	public static final String KEY_NAME = "name";
	public static final int    COL_NAME = 1;
	public static final String KEY_ENABLED = "enabled";
	public static final int    COL_ENABLED = 2;
	public static final String KEY_NEXT = "next";
	public static final int    COL_NEXT = 3;
	public static final String KEY_INTERVAL = "interval";
	public static final int    COL_INTERVAL = 4;
	public static final String KEY_DAYTONE = "dayTone";
	public static final int    COL_DAYTONE = 5;
	public static final String KEY_DAYLED = "dayLED";
	public static final int    COL_DAYLED = 6;
	public static final String KEY_DAYWAIT = "dayWait";
	public static final int    COL_DAYWAIT = 7;
	public static final String KEY_NIGHTTONE = "nightTone";
	public static final int    COL_NIGHTTONE = 8;
	public static final String KEY_NIGHTLED = "nightLED";
	public static final int    COL_NIGHTLED = 9;
	public static final String KEY_NIGHTWAIT = "nightWait";
	public static final int    COL_NIGHTWAIT = 10;
	public static final String KEY_NIGHTSTART = "nightStart";
	public static final int    COL_NIGHTSTART = 11;
	public static final String KEY_NIGHTSTOP = "nightStop";
	public static final int    COL_NIGHTSTOP = 12;
	public static final String KEY_NIGHTNEXT = "nightNext";
	public static final int    COL_NIGHTNEXT = 13;
	
	private static final String DB_CREATE = "create table " + DB_TABLE + " (" +
			KEY_ID + " integer primary key autoincrement, " +
			KEY_NAME + " text not null, " +
			KEY_ENABLED + " integer not null, " +
			KEY_NEXT + " integer not null, " +
			KEY_INTERVAL + " integer not null, " +
			KEY_DAYTONE + " text, " +
			KEY_DAYLED + " integer not null, " +
			KEY_DAYWAIT + " integer not null, " +
			KEY_NIGHTTONE + " text, " +
			KEY_NIGHTLED + " integer not null, " +
			KEY_NIGHTWAIT + " integer not null, " +
			KEY_NIGHTSTART + " integer not null, " +
			KEY_NIGHTSTOP + " integer not null, " +
			KEY_NIGHTNEXT + " integer not null)";

	public ContentValues toContentVals(Timer t)
	{
		ContentValues c = new ContentValues();
		c.put(KEY_NAME, t.name);
		c.put(KEY_ENABLED, t.enabled);
		c.put(KEY_NEXT, t.nextMillis);
		c.put(KEY_INTERVAL, t.intervalSecs);
		c.put(KEY_DAYTONE, t.dayTone == null ? null : t.dayTone.toString());
		c.put(KEY_DAYLED, t.dayLED);
		c.put(KEY_DAYWAIT, t.dayWait);
		c.put(KEY_NIGHTTONE, t.nightTone == null ? null : t.nightTone.toString());
		c.put(KEY_NIGHTLED, t.nightLED);
		c.put(KEY_NIGHTWAIT, t.nightWait);
		c.put(KEY_NIGHTSTART, t.nightStart);
		c.put(KEY_NIGHTSTOP, t.nightStop);
		c.put(KEY_NIGHTNEXT, t.nightNext);
		return c;
	}

	public Timer getEntry(long id)
	{
		Cursor c = db.query(DB_TABLE, null, KEY_ID+"="+id, null, null, null, null);
		if (! c.moveToFirst()) return null;
		Timer t = cursorToEntry(c);
		c.close();
		return t;
	}
	
	public Timer cursorToEntry(Cursor c)
	{
		Timer t = new Timer();
		t.id = c.getLong(COL_ID);
		Log.d(TAG, "cursorToEntry "+t.id);
		t.name = c.getString(COL_NAME);
		t.enabled = c.getInt(COL_ENABLED) > 0;
		t.nextMillis = c.getLong(COL_NEXT);
		t.intervalSecs = c.getInt(COL_INTERVAL);
		t.dayTone = uriOrNull(c.getString(COL_DAYTONE));
		t.dayLED = c.getInt(COL_DAYLED) > 0;
		t.dayWait = c.getInt(COL_DAYWAIT) > 0;
		t.nightTone = uriOrNull(c.getString(COL_NIGHTTONE));
		t.nightLED = c.getInt(COL_NIGHTLED) > 0;
		t.nightWait = c.getInt(COL_NIGHTWAIT) > 0;
		t.nightStart = c.getInt(COL_NIGHTSTART);
		t.nightStop = c.getInt(COL_NIGHTSTOP);
		t.nightNext = c.getInt(COL_NIGHTNEXT) > 0;
		return t;
	}
	
	Uri uriOrNull(String s)
	{
		if (s == null) return null;
		return Uri.parse(s);
	}

	private SQLiteDatabase db;
	private final Context context;
	private Helper helper;
	
	public TimerDB(Context _context)
	{
		context = _context;
		helper = new Helper(context, DB_NAME, null, DB_VERSION);
	}
	
	public TimerDB open() throws SQLException
	{
		db = helper.getWritableDatabase();
		return this;
	}
	
	public void close()
	{
		db.close();
	}
	
	public long saveEntry(Timer t)
	{
		if (t.id < 0) 
			return (t.id = db.insert(DB_TABLE, null, toContentVals(t)));
		else
			return db.update(DB_TABLE, toContentVals(t), KEY_ID+"="+t.id, null);
	}
	
	public int removeEntry(long id)
	{
		return db.delete(DB_TABLE, KEY_ID+"="+id, null);
	}
	
	public int removeEntry(Timer t)
	{
		return removeEntry(t.id);
	}
	
	public Cursor getAllEntries()
	{
		return db.query(DB_TABLE, null, null, null, null, null, KEY_ID);
	}
	
	private static class Helper extends SQLiteOpenHelper
	{
		public Helper(Context context, String name, CursorFactory factory, int version)
		{
			super(context, name, factory, version);
		}

		@Override
		public void onCreate(SQLiteDatabase _db)
		{
			_db.execSQL(DB_CREATE);
		}

		@Override
		public void onUpgrade(SQLiteDatabase _db, int oldV, int newV)
		{
			//_db.execSQL("drop table if exists "+DB_TABLE);
			//onCreate(_db);
		}
	}
}
