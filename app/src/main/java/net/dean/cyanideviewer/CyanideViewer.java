package net.dean.cyanideviewer;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import net.dean.cyanideviewer.db.AuthorDao;
import net.dean.cyanideviewer.db.ComicDao;
import net.dean.cyanideviewer.db.CyanideDatabaseHelper;

/**
 * The main class for Cyanide Viewer.
 */
public class CyanideViewer extends Application {
	/** The tag used by all Log.x methods in this app */
	public static final String TAG = "CyanideViewer";

	/** This application's context */
	private static Context context;

	/** The data access object used to interact with the database */
	private static ComicDao comicDao;

	private static AuthorDao authorDao;

	private static CyanideDatabaseHelper helper;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Starting application");
		CyanideViewer.context = getApplicationContext();

		if (helper == null) {
			helper = new CyanideDatabaseHelper(context);
		}

		CyanideViewer.helper = new CyanideDatabaseHelper(context);
		CyanideViewer.authorDao = new AuthorDao(helper);
		CyanideViewer.comicDao = new ComicDao(helper,  authorDao);
	}

	/** Returns this application's context */
	public static Context getContext() {
		return context;
	}

	/** Returns this application's ComicDao */
	public static ComicDao getComicDao() {
		return comicDao;
	}

	public static AuthorDao getAuthorDao() {
		return authorDao;
	}
}
