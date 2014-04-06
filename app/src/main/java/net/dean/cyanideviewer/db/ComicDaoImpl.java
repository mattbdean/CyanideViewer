package net.dean.cyanideviewer.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import net.dean.cyanideviewer.CyanideUtils;
import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.api.Comic;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements the Comic data access object interface to provide the availability of CRUD operations
 * on a database
 */
public class ComicDaoImpl implements ComicDao {
	/**
	 * The database that will be used to perform CRUD operations on
	 */
	private SQLiteDatabase db;

	/**
	 * The name of the table to use
	 */
	public static final String TABLE_NAME = "comics";

	/**
	 * An array of column names.
	 * [0] => "id"
	 * [1] => "url"
	 * [2] => "is_favorite"
	 */
	private static final String[] COLUMNS = new String[] {"id", "url", "is_favorite"};

	/**
	 * Instantiates a new ComicDaoImpl
	 * @param context The context to use
	 */
	public ComicDaoImpl(Context context) {
		this.db = new ComicDatabaseHelper(context).getWritableDatabase();
	}

	@Override
	public List<Comic> getAllComics() {
		Cursor cursor = db.query(TABLE_NAME, COLUMNS,
				null, null, null, null, null);

		List<Comic> comics = new ArrayList<>();

		while (!cursor.isAfterLast()) {
			comics.add(parseComic(cursor));
			cursor.moveToNext();
		}
		cursor.close();

		return comics;
	}

	@Override
	public ArrayList<Comic> getFavoriteComics() {
		// SELECT * FROM TABLE_NAME WHERE is_favorite>0
		Cursor cursor = db.query(true, TABLE_NAME, COLUMNS, COLUMNS[2] + ">0",
				null, null, null, COLUMNS[0]+" DESC", null);

		ArrayList<Comic> favorites = new ArrayList<>();

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			favorites.add(parseComic(cursor));
			cursor.moveToNext();
		}

		return favorites;
	}

	@Override
	public Comic getComic(long id) {
		// SELECT * FROM TABLE_NAME WHERE id=$id
		Cursor cursor = db.query(true, TABLE_NAME, COLUMNS, COLUMNS[0] + "=?",
				new String[] {Long.toString(id)}, null, null, null, null);

		cursor.moveToFirst();
		Comic c = parseComic(cursor);
		cursor.close();
		return c;
	}

	@Override
	public boolean comicExists(long id) {
		Cursor cursor = db.query(true, TABLE_NAME, COLUMNS, COLUMNS[0] + "=?",
				new String[] {Long.toString(id)}, null, null, null, null);
		boolean exists = cursor.getCount() > 0;
		cursor.close();
		return exists;
	}

	@Override
	public void addComic(Comic c) {
		if (comicExists(c.getId())) {
			return;
		}

		Log.v(CyanideViewer.TAG, "Adding comic to the database: " + c);

		ContentValues values = new ContentValues();
		values.put(COLUMNS[0], c.getId());
		values.put(COLUMNS[1], c.getUrl().toExternalForm());
		values.put(COLUMNS[2], c.isFavorite());

		db.insert(TABLE_NAME, null, values);
	}

	@Override
	public void updateComicAsFavorite(Comic c) {
		ContentValues values = new ContentValues();
		values.put(COLUMNS[2], c.isFavorite());

		db.update(TABLE_NAME, values, COLUMNS[0] + "=?", new String[] {Long.toString(c.getId())});
	}

	@Override
	public void deleteComic(Comic c) {
		Log.w(CyanideViewer.TAG, "Deleting comic :" + c);
		db.delete(TABLE_NAME, COLUMNS[0] + " = ?", new String[] {Long.toString(c.getId())});
	}

	/**
	 * Parses a Comic given a Cursor from the 'comics' table of the database
	 * @param c A new Comic parsed from the values of the row at which the cursor is pointing
	 * @return A new comic from a cursor
	 */
	private Comic parseComic(Cursor c) {
		Comic comic = new Comic(-1, null);
		if (c.isAfterLast()) {
			return null;
		}
		comic.setId(c.getLong(c.getColumnIndex(COLUMNS[0])));
		comic.setUrl(CyanideUtils.newUrl(c.getString(c.getColumnIndex(COLUMNS[1]))));
		comic.setFavorite(c.getInt(c.getColumnIndex(COLUMNS[2])) > 0);

		return comic;
	}

	@Override
	public void deleteAllComics() {
		// http://stackoverflow.com/a/6835115
		db.execSQL("DELETE FROM " + TABLE_NAME);
		db.execSQL("VACUUM");
	}
}
