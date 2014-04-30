package net.dean.cyanideviewer.api;

import android.util.Log;

import net.dean.cyanideviewer.Callback;
import net.dean.cyanideviewer.CyanideUtils;
import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.api.comic.Author;
import net.dean.cyanideviewer.api.comic.Comic;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides high level access to the unofficial Cyanide and Happiness API.
 */
public class CyanideApi extends BaseComicApi {
	private static final String BASE_URL = "https://explosm.net/comics/";

	/** The one and only instance of CyanideApi  */
	private static CyanideApi instance;

	private static final DateFormat dateFormat = new SimpleDateFormat("MM.d.yyyy");

	/** Retrieves the one and only instance of CyanideApi */
	public static CyanideApi instance() {
		return instance;
	}

	public static void setUp() {
		if (instance != null) {
			Log.w(CyanideViewer.TAG, "Setting up CyanideApi more than one time");
			return;
		}

		instance = new CyanideApi();
		instance.firstId = instance.getIdFromUrl(SpecialSelection.FIRST.getUrl());
		Log.i(CyanideViewer.TAG, "Found first ID: " + instance.firstId);
		instance.newestId = getNewestComicId();
		Log.i(CyanideViewer.TAG, "Found newest ID: " + instance.newestId);
	}

	/** The ID of the very first publicly available C&H comic */
	private long firstId;

	/** The ID of the newest C&H comic */
	private long newestId;


	@Override
	public long getIdFromUrl(String url) {
		String currentUrl = followRedirect(url);
		// Regex for the explosm website. Possible matches:
		// http://explosm.net/comics/3530
		// http://explosm.net/comics/3530/
		// http://www.explosm.net/comics/3530/
		if (!currentUrl.matches("http(s)?://(www\\.)?explosm\\.net/comics/\\d*(/)?")) {
			Log.e(CyanideViewer.TAG, "Cannot load comic: URL not in correct format: " + currentUrl);
			return -1;
		}
		return Long.parseLong(currentUrl.substring(currentUrl.lastIndexOf('/', currentUrl.length() - 2)).replace("/", ""));
	}

	@Override
	public String getBitmapUrl(long id) {
		return getBitmapUrl(getBaseUrl() + id);
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

		if (getBitmapUrl(newId) != null) {
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

		if (getBitmapUrl(newId) != null) {
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
		if (getBitmapUrl(newestId) != null) {
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
		Comic c = new Comic(id, null, null, null);

		if (CyanideViewer.getComicDao().exists(id)) {
			// The comic was found in the database
			Log.i(CyanideViewer.TAG, "Comic #" + id + " was found on the database, using it's info");
			c = CyanideViewer.getComicDao().get(id);
		} else {
			String url = getBitmapUrl(id);
			if (url != null) {
				// The wasn't in the database, add it
				Log.i(CyanideViewer.TAG, "Comic #" + id + " was not found in the database.");
				c = fillMetadata(c);
				CyanideViewer.getComicDao().add(c);
			}
		}


		// Check if the comic exists locally
		File localComic = getLocalComic(id);
		if (localComic != null) {
			try {
				URL url = localComic.toURI().toURL();
				c.setUrl(url);
				Log.i(CyanideViewer.TAG, "Using comic on filesystem for #" + id + ": " + url.toExternalForm());
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
		return BASE_URL;
	}

	@Override
	public long getFirstId() {
		return firstId;
	}

	/**
	 * Sets a comic's bitmap URL, author, and date published if they have not been set already.
	 * @param c The comic to fill
	 * @return A variant of the given comic that has it's metadata filled out
	 */
	private Comic fillMetadata(Comic c) {
		if (c.getUrl() == null || c.getAuthor() == null || c.getPublished() == null) {
			try {
				Document doc = Jsoup.connect(getBaseUrl() + c.getId()).get();
				if (c.getUrl() == null) {
					c.setUrl(CyanideUtils.newUrl(doc.select("#maincontent img[src*=/db/files/Comics/]").get(0).attr("src")));
				}
				if (c.getPublished() == null || c.getAuthor() == null) {
					// "by", "Dave", "McElfatrick", "04.22.2014"
					String[] parts = doc.select("nobr").get(0).text().split(" ");

					if (c.getPublished() == null) {
						String date = parts[3];
						try {
							// Parse the date
							c.setPublished(dateFormat.parse(date));
						} catch (ParseException e) {
							Log.e(CyanideViewer.TAG, "Unable to parse " + date + " to a date.", e);
						}
					}

					if (c.getAuthor() == null) {
						// "Dave" + "McElfatrick"
						c.setAuthor(Author.getByName(parts[1] + " " + parts[2]));
					}
				}
			} catch (IOException e) {
				Log.e(CyanideViewer.TAG, "Unable to fill in comic metadata for #" + c.getId());
				return null;
			}
		}

		return c;
	}

	/**
	 * Tries to find the bitmap URL from a URL such as "http://explosm.net/comics/1234"
	 * @param comicUrl The URL to use
	 * @return The URL of the comic's image
	 */
	private static String getBitmapUrl(String comicUrl) {
		try {
			Document doc = Jsoup.connect(comicUrl).get();

			// Return the one image that contains "/db/files/Comics/" in the src attribute
			return doc.select("#maincontent img[src*=/db/files/Comics/]").get(0).attr("src");
		} catch (IndexOutOfBoundsException e) {
			Log.w(CyanideViewer.TAG, "Could not find the comic's image on " + comicUrl);
		} catch (IOException e) {
			Log.e(CyanideViewer.TAG, "IOException while trying to find the bitmap URL of the comic at " + comicUrl, e);
			// TODO Error recovery. Notify the user somehow. Maybe a 'retry' button?
		}

		return null;
	}

	/**
	 * Gets the newest comic ID
	 * @return The newest ID
	 */
	private static long getNewestComicId() {
		long newestValidComicId = instance().getIdFromUrl(SpecialSelection.NEWEST.getUrl());
		boolean needsToSearch = true;
		while (needsToSearch) {
			String bitmapUrl = getBitmapUrl(BASE_URL + newestValidComicId);
			if (bitmapUrl == null) {
				// There was no comic on that page
				newestValidComicId--;
				if (newestValidComicId < 0) {
					Log.e(CyanideViewer.TAG, "Search for the newest valid comic ID went negative, aborting.");
					return -1;
				}
				continue;
			}

			// The comic page was valid, no need to search anymore
			needsToSearch = false;
		}

		return newestValidComicId;
	}

	@Override
	public void checkForNewComic(Callback<Boolean> callback) {
		long latest = getNewestComicId();
		boolean hasNewer = latest > newestId && latest != -1;
		if (latest != -1) {
			newestId = latest;
		}
		callback.onComplete(hasNewer);
	}
}
