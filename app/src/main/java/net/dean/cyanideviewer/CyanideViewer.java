package net.dean.cyanideviewer;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import net.dean.cyanideviewer.api.CyanideApi;
import net.dean.cyanideviewer.db.ComicDaoImpl;

/**
 * The main class for Cyanide Viewer.
 */
public class CyanideViewer extends Application {
	/** The tag used by all Log.x methods in this app */
	public static final String TAG = "CyanideViewer";

	/** The application context */
	private static Context context;

	/** The data access object used to interact with the database */
	private static ComicDaoImpl comicDao;

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Starting application");
		CyanideViewer.context = getApplicationContext();
		CyanideViewer.comicDao = new ComicDaoImpl(context);
		CyanideApi.initIdRanges();
	}

	/** Returns this application's context */
	public static Context getContext() {
		return context;
	}

	/** Returns this application's ComicDao */
	public static ComicDaoImpl getComicDao() {
		return comicDao;
	}
}