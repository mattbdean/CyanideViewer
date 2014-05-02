package net.dean.cyanideviewer.db;

import android.database.Cursor;
import android.util.Log;

import net.dean.cyanideviewer.CyanideUtils;
import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.api.comic.Comic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Extends the base data access object to provide the availability of CRUD operations
 * of Comic objects
 *
 * Columns: "id", "url", "is_favorite", "published", and "author_id"
 */
public class ComicDao extends BaseDao<Comic> {

	private AuthorDao authorDao;

	public ComicDao(CyanideDatabaseHelper helper, AuthorDao authorDao) {
		super(helper, CyanideDatabaseHelper.TABLE_COMICS, Comic.class);
		this.authorDao = authorDao;
	}

	/**
	 * Gets a list of the user's favorite comics
	 * @return A list of the user's favorite comics
	 */
	public ArrayList<Comic> getFavoriteComics() {
		// SELECT * FROM comics WHERE is_favorite>0
		Cursor cursor = db.query(true, tableName, columns, columns[2] + ">0",
				null, null, null, columns[0]+" DESC", null);

		ArrayList<Comic> favorites = new ArrayList<>();

		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			favorites.add(parse(cursor));
			cursor.moveToNext();
		}

		return favorites;
	}

	@Override
	public boolean delete(long id) {
		Log.w(CyanideViewer.TAG, "Deleting comic :" + id);
		return super.delete(id);
	}

	@Override
	protected Comic parse(Cursor c) {
		Comic comic = new Comic(-1, null, null, null);
		if (c.isAfterLast()) {
			return null;
		}

		List<String> fields = Comic.getDatabaseFieldNames(Comic.class);
		for (String key : fields) {
			int columnIndex = c.getColumnIndex(key);
			switch (key) {
				case "id":
					comic.setId(c.getLong(columnIndex));
					break;
				case "url":
					comic.setUrl(CyanideUtils.newUrl(c.getString(columnIndex)));
					break;
				case "is_favorite":
					comic.setFavorite(c.getInt(columnIndex) > 0);
					break;
				case "published":
					comic.setPublished(new Date(c.getLong(columnIndex))); // Stored in unix time
					break;
				case "author_id":
					// Get an Author via AuthorDao
					comic.setAuthor(authorDao.get(c.getLong(columnIndex)));
					break;
				case "bitmap_hash":
					comic.setBitmapHash(c.getString(columnIndex));
					break;
				case "icon_hash":
					comic.setIconHash(c.getString(columnIndex));
					break;
				default:
					Log.w(CyanideViewer.TAG, "Unknown column: " + key);
			}
		}

		return comic;
	}
}
