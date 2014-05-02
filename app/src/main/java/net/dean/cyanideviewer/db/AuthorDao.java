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
		Author author = new Author(-1, null, null, null);
		if (c.isAfterLast()) {
			return null;
		}

		List<String> fields = Author.getDatabaseFieldNames(Author.class);
		for (String key : fields) {
			int columnIndex = c.getColumnIndex(key);
			switch (key) {
				case "id":
					author.setId(c.getLong(columnIndex));
					break;
				case "name":
					author.setName(c.getString(columnIndex));
					break;
				case "twitter":
					author.setTwitter(c.getString(columnIndex));
					break;
				case "facebook":
					author.setFacebook(c.getString(columnIndex));
					break;
				default:
					Log.w(Constants.TAG, "Unknown column: " + key);
			}
		}

		return author;
	}

	@Override
	protected boolean isValid(Author model) {
		return false; // add() not used, this method is irrelevant
	}
}
