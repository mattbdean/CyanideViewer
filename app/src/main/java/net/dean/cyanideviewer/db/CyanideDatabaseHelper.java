package net.dean.cyanideviewer.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import net.dean.cyanideviewer.Constants;

/**
 * This class is used to open SQLite databases
 */
public class CyanideDatabaseHelper extends SQLiteOpenHelper {

	/** The name of the database */
	private static final String DB_NAME = "comics.db";

	static final String TABLE_COMICS = "comics";

	static final String TABLE_AUTHORS = "authors";

	/** The SQLite version of the database to use */
	private static final int DB_VERSION = 1;

	/** The SQL query to use when creating the 'comics' table */
	// I really don't know how to format this...
	private static final String CREATE_COMICS = String.format(
			"CREATE TABLE IF NOT EXISTS %s (" +
					"id INTEGER," +
					"url TEXT," +
					"is_favorite INTEGER," +
					"published INTEGER," +
					"author_id INTEGER," +
					"bitmap_hash TEXT," +
					"icon_hash TEXT," +
					"FOREIGN KEY(author_id) REFERENCES %s(id)" +
			");", TABLE_COMICS, TABLE_AUTHORS);

	private static final String CREATE_AUTHORS = String.format(
			"CREATE TABLE IF NOT EXISTS %s (" +
					"id INTEGER PRIMARY KEY," +
					"name TEXT," +
					"twitter TEXT," +
					"facebook TEXT" +
			")", TABLE_AUTHORS);

	private static final String INSERT_AUTHORS =
			"INSERT INTO authors (id, name, twitter, facebook) VALUES" +
					"(0, 'Kris Wilson',      'TheKrisWilson', '96452604801')," +
					"(1, 'Rob DenBleyker',   'RobDenBleyker', 'robdenbleyker')," +
					"(2, 'Matt Melvin',      'MattMelvin',    'RobotsWithFeelings')," +
					"(3, 'Dave McElfatrick', 'daveexplosm',   '143006940192');";


	/**
	 * Instantiates a new ComicDatabaseHelper
	 * @param context The context to use
	 */
	public CyanideDatabaseHelper(Context context) {
		this(context, false);
	}

	public CyanideDatabaseHelper(Context context, boolean isMemory) {
		super(context, isMemory ? null : DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// Accessed but not created yet
		db.execSQL(CREATE_AUTHORS);
		db.execSQL(CREATE_COMICS);
		db.execSQL(INSERT_AUTHORS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(Constants.TAG_DB, "Upgrading database from version " + oldVersion + " to " + newVersion
						+ ", which will destroy all old data");

		// Drop all tables
		for (String table : BaseDao.getTableNames()) {
			db.execSQL("DROP TABLE IF EXISTS " + table);
		}

		onCreate(db);
	}
}
