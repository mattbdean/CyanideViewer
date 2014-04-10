package net.dean.cyanideviewer.api;

import android.os.AsyncTask;
import android.util.Log;

import net.dean.cyanideviewer.CyanideUtils;
import net.dean.cyanideviewer.CyanideViewer;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Provides high level access to the unofficial Cyanide and Happiness API.
 */
public class CyanideApi extends BaseComicApi {
	/** The one and only instance of CyanideApi  */
	private static CyanideApi instance;

	/** Retrieves the one and only instance of CyanideApi */
	public static CyanideApi instance() {
		if (instance == null) {
			instance = new CyanideApi();
			initIdRanges();
		}
		return instance;
	}

	/** The ID of the very first publicly available C&H comic */
	private long firstId;

	/** The ID of the newest C&H comic */
	private long newestId;


	@Override
	public long getIdFromUrl(String url) {
		String currentUrl = followRedirect(url);
		if (!currentUrl.contains("explosm.net/comics/")) {
			Log.e(CyanideViewer.TAG, "Cannot load comic: URL not in correct format: " + currentUrl);
			return -1;
		}
		return Long.parseLong(currentUrl.substring(currentUrl.lastIndexOf('/', currentUrl.length() - 2)).replace("/", ""));
	}

	@Override
	public String getComicUrl(long id) {
		try {
			Document doc = Jsoup.connect(getBaseUrl() + id).get();

			// Return the one image that contains "/db/files/Comics/" in the src attribute
			return doc.select("#maincontent img[src*=/db/files/Comics/]").get(0).attr("src");
		} catch (IndexOutOfBoundsException e) {
			Log.w(CyanideViewer.TAG, "Could not find the comic's image for #" + id);
		} catch (IOException e) {
			Log.e(CyanideViewer.TAG, "IOException while testing if comic #" + id + " was a comic page or not", e);
			// TODO Error recovery. Notify the user somehow. Maybe a 'retry' button?
		}

		return null;
	}

	// TODO: getPrevious() and getNext() are pretty much the same method; they can be shortened
	@Override
	public Comic getPrevious(long relativeToId) {
		long newId = relativeToId - 1;

		Log.i(CyanideViewer.TAG, "Getting the previous comic relative to #" + relativeToId);
		if (newId < firstId) {
			// The ID is less than the first comic's ID
			Log.w(CyanideViewer.TAG, String.format("The given ID (%s) was lower than the minimum ID (%s)", newId, firstId));
			return getComic(firstId);
		}

		if (newId > newestId) {
			// The ID is greater than the newest comic's ID
			Log.w(CyanideViewer.TAG, String.format("The given ID (%s) was greater than the maximum ID (%s)", newId, newestId));
			return getComic(newestId);
		}

		if (getComicUrl(newId) != null) {
			// There is a comic associated with this page
			// TODO: Add an optional param for getComic(), we already know what the comic URL is and
			// getComic() is just going to get it again
			return getComic(newId);
		}

		Log.i(CyanideViewer.TAG, "There was no comic associated with comic #" + newId + ", trying #" + (newId - 1));
		// Go to the previous comic
		return getPrevious(newId);
	}

	@Override
	public Comic getNext(long relativeToId) {
		long newId = relativeToId + 1;

		Log.i(CyanideViewer.TAG, "Getting the next comic relative to #" + relativeToId);
		if (newId < firstId) {
			// The ID is less than the first comic's ID
			Log.w(CyanideViewer.TAG, String.format("The given ID (%s) was lower than the minimum ID (%s)", newId, firstId));
			return null;
		}

		if (newId > newestId) {
			// The ID is greater than the newest comic's ID
			Log.w(CyanideViewer.TAG, String.format("The given ID (%s) was greater than the maximum ID (%s)", newId, newestId));
			return null;
		}

		if (getComicUrl(newId) != null) {
			// There is a comic associated with this page
			// TODO: Add an optional param for getComic(), we already know what the comic URL is and
			// getComic() is just going to get it again
			return getComic(newId);
		}

		Log.i(CyanideViewer.TAG, "There was no comic associated with comic #" + newId + ", trying #" + (newId + 1));
		// Go to the previous comic
		return getNext(newId);
	}

	@Override
	public Comic getNewest() {
		if (getComicUrl(newestId) != null) {
			// The newest comic is an image
			return getComic(newestId);
		}
		// The newest comic is a short or does not exist
		return getPrevious(newestId);

	}

	@Override
	public Comic getFirst() {
		return getComic(firstId);
	}

	@Override
	public Comic getRandom() {
		return getComic(getIdFromUrl(followRedirect(SpecialSelection.RANDOM.getUrl())));
	}

	@Override
	public boolean getSupportsRandomComics() {
		// Will always be true
		return true;
	}

	@Override
	public File getLocalComic(long id) {
		if (!getSavedImageDirectory().isDirectory()) {
			if (!getSavedImageDirectory().mkdirs()) {
				// Prevent IllegalArgumentException by making sure this location is a directory
				Log.e(CyanideViewer.TAG, "Unable to create the directory " + getSavedImageDirectory().getAbsolutePath() +
						". Does it exist as a file?");
			}
		}
		List<File> files = new ArrayList<>(FileUtils.listFiles(getSavedImageDirectory(),
				new String[] {"jpg", "jpeg", "png", "gif"}, false));
		for (File f : files) {
			// ex: 3496
			String lookingFor = f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('/') + 1, f.getAbsolutePath().indexOf('.'));
			String idString = Long.toString(id);
			if (lookingFor.equals(idString)) {
				// Found the local file
				return f;
			}
		}

		return null;
	}

	@Override
	public Comic getComic(long id) {
		Comic c = new Comic(id, null, false);

		if (CyanideViewer.getComicDao().comicExists(id)) {
			// The comic was found in the database
			Log.i(CyanideViewer.TAG, "Comic #" + id + " was found on the database, using it's info");
			c = CyanideViewer.getComicDao().getComic(id);
		} else {
			// The wasn't in the database, add it
			Log.i(CyanideViewer.TAG, "Comic #" + id + " was not found in the database.");
			c.setUrl(CyanideUtils.newUrl(getComicUrl(id)));
			CyanideViewer.getComicDao().addComic(c);
		}


		// Check if the comic exists locally
		File localComic = getLocalComic(id);
		if (localComic != null) {
			try {
				URL url = localComic.toURI().toURL();
				c.setUrl(url);
				Log.i(CyanideViewer.TAG, "Using comic on filesystem for #" + id + ": " + url.toExternalForm());
				return new Comic(id, url);
			} catch (MalformedURLException e) {
				Log.e(CyanideViewer.TAG, "Malformed URL: " + localComic.getAbsolutePath(), e);
			}
		}

		return c;
	}

	@Override
	public long getNewestId() {
		return newestId;
	}

	@Override
	public String getBaseUrl() {
		return "http://explosm.net/comics/";
	}

	@Override
	public long getFirstId() {
		return firstId;
	}

	private static void initIdRanges() {
		try {
			instance.firstId = new AsyncTask<Void, Void, Long>() {
				@Override
				protected Long doInBackground(Void... params) {
					long id = instance().getIdFromUrl(SpecialSelection.FIRST.getUrl());
					return instance().getIdFromUrl(SpecialSelection.FIRST.getUrl());
				}
			}.execute().get();
			Log.i(CyanideViewer.TAG, "Found first ID: " + instance.firstId);

			instance.newestId = new AsyncTask<Void, Void, Long>() {

				@Override
				protected Long doInBackground(Void... params) {
					return instance().getIdFromUrl(SpecialSelection.NEWEST.getUrl());
				}
			}.execute().get();
			Log.i(CyanideViewer.TAG, "Found newest ID: " + instance.newestId);
		} catch (InterruptedException | ExecutionException e) {
			Log.e(CyanideViewer.TAG, "Failed to find the first or newest comic IDs", e);
		}
	}
}
