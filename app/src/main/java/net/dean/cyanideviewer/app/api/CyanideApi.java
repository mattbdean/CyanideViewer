package net.dean.cyanideviewer.app.api;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import android.util.Log;

import net.dean.cyanideviewer.app.CyanideUtils;
import net.dean.cyanideviewer.app.CyanideViewer;
import net.dean.cyanideviewer.app.R;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Provides high level access to the unofficial Cyanide and Happiness API.
 */
public class CyanideApi {
	/** The base URL for the Cyanide and Happiness comic. */
	public static final String BASE_URL = "http://explosm.net/comics/";

	/** The ID of the very first publicly available C&H comic */
	public static long firstId;

	/** The ID of the newest C&H comic */
	public static long newestId;

	/** The HttpClient that will be used to host requests */
	private static HttpClient client = new DefaultHttpClient();

	/** The directory that this app will download comics to. Sample value: "/sdcard/CyanideViewer/" */
	public static final File IMAGE_DIR = new File(Environment.getExternalStorageDirectory(),
			CyanideViewer.getContext().getResources().getString(R.string.app_name));

	private CyanideApi() {
		// Prevent instances
	}

	/**
	 * Checks if the current code is being executed on the UI thread. If it is, it logs a
	 * NetworkOnMainThreadException.
	 */
	private static void checkMainThread() {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			Log.e(CyanideViewer.TAG, "Networking on main thread", new NetworkOnMainThreadException());
		}
	}

	/**
	 * Parses an ID from a URL. For example, http://explosm.net/comics/1234 would return 1234.
	 * @param url The URL to parse
	 * @return The ID of the given comic URL
	 */
	public static Long parseId(String url) {
		String currentUrl = followRedirect(url);
		if (!currentUrl.contains("explosm.net/comics/")) {
			Log.e(CyanideViewer.TAG, "Cannot load comic: URL not in correct format: " + currentUrl);
			return null;
		}
		return Long.parseLong(currentUrl.substring(currentUrl.lastIndexOf('/', currentUrl.length() - 2)).replace("/", ""));
	}

	/**
	 * Follows a given URL and returns the URL that the request was redirected to.
	 * @param url The URL to follow
	 * @return The URL that the given URL will redirect to
	 */
	private static String followRedirect(String url) {
		checkMainThread();

		try {
			new URL(url).toURI();
		} catch (MalformedURLException | URISyntaxException e) {
			Log.e(CyanideViewer.TAG, "Malformed URL: " + url, e);
			return null;
		}

		try {
			HttpGet get = new HttpGet(url);
			HttpContext context = new BasicHttpContext();
			HttpResponse response = client.execute(get, context);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				throw new IOException(response.getStatusLine().toString());
			}
			HttpUriRequest currentReq = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
			HttpHost currentHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
			String currentUrl = (currentReq.getURI().isAbsolute() ? currentReq.getURI().toString() : (currentHost.toURI() + currentReq.getURI()));
			Log.i(CyanideViewer.TAG, "Followed \"" + url + "\" to \"" + currentUrl + "\"");
			return currentUrl;
		} catch (IOException e) {
			Log.e(CyanideViewer.TAG, "Unable to get the latest comic ID", e);
			return null;
		}
	}

	/**
	 * Gets the URL of a Comic from the internet.
	 * @param id The ID of the comic to look up
	 * @return A String representing a URL, or null if the comic does not exist.
	 */
	public static String getComicUrl(long id) {
		checkMainThread();

		try {
			Document doc = Jsoup.connect(BASE_URL + id).get();
			Elements images = doc.select("#maincontent img[src]");

			for (Element e : images) {
				if (e.hasAttr("src")) {
					if (e.attr("src").contains("/db/files/Comics/")) {
						return e.attr("src");
					}
				}
			}
		} catch (IOException e) {
			Log.e(CyanideViewer.TAG, "IOException while testing if comic #" + id + " was a comic page or not", e);
		}

		return null;
	}

	// TODO: getPrevious() and getNext() are pretty much the same method; they can be shortened

	/**
	 * Gets the comic relative to the of the comic given.
	 * @param id The ID of the comic to get the comic previous to
	 * @return The comic before the given ID
	 */
	public static Comic getPrevious(long id) {
		long newId = id - 1;

		Log.i(CyanideViewer.TAG, "Getting the previous comic relative to #" + id);
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

	/**
	 * Gets the next comic relative to a certain ID
	 * @param id The ID to use
	 * @return The comic next in line from the given ID
	 */
	public static Comic getNext(long id) {
		long newId = id + 1;

		Log.i(CyanideViewer.TAG, "Getting the previous comic relative to #" + id);
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

		Log.i(CyanideViewer.TAG, "There was no comic associated with comic #" + newId + ", trying #" + (newId + 1));
		// Go to the previous comic
		return getNext(newId);
	}

	/**
	 * Gets the newest comic
	 * @return The newest comic
	 */
	public static Comic getNewest() {
		if (getComicUrl(newestId) != null) {
			// The newest comic is an image
			return getComic(newestId);
		}
		// The newest comic is an short or does not exist
		return getPrevious(newestId);

	}

	/**
	 * Gets the first comic
	 * @return The first comic
	 */
	public static Comic getFirst() {
		return getComic(firstId);
	}

	/**
	 * Gets a random comic
	 * @return A random comic
	 */
	public static Comic getRandom() {
		return getComic(parseId(followRedirect(SpecialSelection.RANDOM.getUrl())));
	}

	/**
	 * Tests if a comic with a certain ID has been downloaded
	 * @param id The ID to use
	 * @return Whether the comic has been downloaded
	 */
	public static boolean hasLocal(long id) {
		return getLocalComic(id) != null;
	}

	/**
	 * Gets the location of a comic on the filesystem.
	 * @param id The ID of the comic to find
	 * @return A File pointing to the comic represented by the given ID
	 */
	private static File getLocalComic(long id) {
		List<File> files = (List) FileUtils.listFiles(IMAGE_DIR, new String[] {"jpg", "jpeg", "png"}, false);
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

	/**
	 * Gets a Comic object based on the given ID. If the comic exists in the database, it will
	 * return the data stored in that row. If a row for that particular comic does not exist,
	 * one will be added. If a file
	 * @param id The ID of the comic to find
	 * @return A new Comic based on the current ID, or null if that comic does not exist.
	 */
	public static Comic getComic(long id) {
		Comic c = new Comic(id, null, false);

		if (CyanideViewer.getComicDao().comicExists(id)) {
			// The comic was found in the database
			Log.i(CyanideViewer.TAG, "Comic #" + id + " was found on the database, using it's info");
			c = CyanideViewer.getComicDao().getComic(id);
		} else {
			// The wasn't in the database, add it
			Log.i(CyanideViewer.TAG, "Comic #" + id + " was not found in the database.");
			String str = getComicUrl(id);
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

	public static void initIdRanges() {
		// Get the first and last comics available

		try {
			firstId = new AsyncTask<Void, Void, Long>() {
				@Override
				protected Long doInBackground(Void... params) {
					return parseId(SpecialSelection.FIRST.getUrl());
				}
			}.execute().get();
			Log.i(CyanideViewer.TAG, "Found first ID: " + firstId);

			newestId = new AsyncTask<Void, Void, Long>() {

				@Override
				protected Long doInBackground(Void... params) {
					return parseId(SpecialSelection.NEWEST.getUrl());
				}
			}.execute().get();
			Log.i(CyanideViewer.TAG, "Found newest ID: " + newestId);
		} catch (InterruptedException | ExecutionException e) {
			Log.e(CyanideViewer.TAG, "Failed to find the first or newest comic IDs", e);
		}
	}
}
