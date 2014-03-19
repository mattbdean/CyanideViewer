package net.dean.cyanideviewer.app.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import net.dean.cyanideviewer.app.CyanideViewer;

public class ComicDatabaseHelper extends SQLiteOpenHelper {
	private static final String DB_NAME = "comics";
	private static final int DB_VERSION = 3;
	private static final String DB_CREATE_SQL =
			String.format("CREATE TABLE IF NOT EXISTS %s (id INTEGER, url TEXT, is_favorite INTEGER);", DB_NAME);

	public ComicDatabaseHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Accessed but not created yet
		db.execSQL(DB_CREATE_SQL);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(CyanideViewer.TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
						+ ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS comics");
		onCreate(db);
	}
}
