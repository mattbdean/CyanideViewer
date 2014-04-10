package net.dean.cyanideviewer.api;

import android.os.Environment;
import android.util.Log;

import net.dean.cyanideviewer.CyanideViewer;

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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * This is a basic implementation of ComicApi that provides some default methods that should work
 * for all comics. This class comes equipped with an HttpClient and fills out hasLocalComic(),
 * getSavedImageDirectory(), and getSavedIconDirectory(). It also contains a few utility methods.
 */
public abstract class BaseComicApi implements ComicApi {
	/** The directory that will be used to write images to */
	private static final File IMAGE_DIR = new File(Environment.getExternalStorageDirectory(), "CyanideViewer");

	/**
	 * The directory taht will be used to write icons to. This is a subdirectory of {@link #IMAGE_DIR}
	 * called "icons"
	 */
	private static final File ICON_DIR = new File(IMAGE_DIR, "icons");

	/** The HttpClient that will be used to make requests */
	protected HttpClient client;

	/**
	 * Instantiates a new BaseComicApi
	 */
	public BaseComicApi() {
		this.client = new DefaultHttpClient();
	}

	/**
	 * Gets the end result of an HTTP GET request. The URL provided is followed and the resulting
	 * location is returned. For example, if "http://explosm.net/comics/random" redirected to
	 * "http://explosm.net/comics/3123", then the latter value would be returned. If a URL does
	 * not redirect, the original URL will be returned. For example, if "http://example.com" did
	 * not redirect to another webpage, that value would be returned.
	 * @param url The URL to follow
	 * @return The destination of an HTTP GET request
	 */
	protected String followRedirect(String url) {
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
			response.getEntity().consumeContent();
			Log.i(CyanideViewer.TAG, "Followed \"" + url + "\" to \"" + currentUrl + "\"");
			return currentUrl;
		} catch (IOException e) {
			Log.e(CyanideViewer.TAG, "Unable to get the latest comic ID", e);
			return null;
		}
	}

	@Override
	public boolean hasLocalComic(long id) {
		// Generic method
		return getLocalComic(id) != null;
	}

	@Override
	public Comic getFirst() {
		// Generic function
		return getComic(getFirstId());
	}

	@Override
	public Comic getNewest() {
		// Generic function
		return getComic(getNewestId());
	}

	// Since getSavedImageDirectory() is called a lot, create one instance of the image directory
	// to speed things up, since it relies on a potentially expensive call (Environment.getExternalStorageDirectory())

	@Override
	public File getSavedImageDirectory() {
		return IMAGE_DIR;
	}

	@Override
	public File getSavedIconDirectory() {
		return ICON_DIR;
	}
}
