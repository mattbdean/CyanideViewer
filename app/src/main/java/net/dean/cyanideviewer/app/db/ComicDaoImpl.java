package net.dean.cyanideviewer.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import net.dean.cyanideviewer.app.CyanideViewer;
import net.dean.cyanideviewer.app.api.Comic;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ComicDaoImpl implements ComicDao {
	private SQLiteDatabase db;

	private static final String TABLE_NAME = "comics";
	private static final String[] COLUMNS = new String[] {"id", "url", "is_favorite"};

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
	public List<Comic> getFavoriteComics() {
		List<Comic> favorites = new ArrayList<>();

		for (Comic c : getAllComics())
			if (c.isFavorite())
				favorites.add(c);

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

	private Comic parseComic(Cursor c) {
		Comic comic = new Comic(-1, null);
		if (c.isAfterLast()) {
			return null;
		}
		comic.setId(c.getLong(c.getColumnIndex(COLUMNS[0])));
		try {
			comic.setUrl(new URL(c.getString(c.getColumnIndex(COLUMNS[1]))));
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		comic.setFavorite(c.getInt(c.getColumnIndex(COLUMNS[2])) > 0);

		return comic;
	}
}
