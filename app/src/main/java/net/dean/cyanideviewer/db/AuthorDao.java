package net.dean.cyanideviewer.db;

import android.database.Cursor;
import android.util.Log;

import net.dean.cyanideviewer.Constants;
import net.dean.cyanideviewer.api.comic.Author;

import java.util.List;


/**
 * Extends the base data access object to read and parse Author objects
 *
 * Columns: "id", "name", "twitter", "facebook", not guaranteed to be in that order
 */
public class AuthorDao extends BaseDao<Author> {

	public AuthorDao(CyanideDatabaseHelper helper) {
		super(helper, CyanideDatabaseHelper.TABLE_AUTHORS, Author.class, false);
	}


	@Override
	protected Author parse(Cursor c) {
		if (c.isAfterLast()) {
			return null;
		}

		// Don't instantiate the Author until we have enough information
		long id = -1;
		String name = null;
		String twitter = null;
		String facebook = null;


		List<String> fields = Author.getDatabaseFieldNames(Author.class);
		for (String key : fields) {
			int columnIndex = c.getColumnIndex(key);
			switch (key) {
				case "id":
					id = c.getLong(columnIndex);
					break;
				case "name":
					name = c.getString(columnIndex);
					break;
				case "twitter":
					twitter = c.getString(columnIndex);
					break;
				case "facebook":
					facebook = c.getString(columnIndex);
					break;
				default:
					Log.w(Constants.TAG_DB, "Unknown column: " + key);
			}
		}

		return new Author(id, name, twitter, facebook);
	}

	@Override
	protected boolean isValid(Author model) {
		return false; // add() not used, this method is irrelevant
	}
}
