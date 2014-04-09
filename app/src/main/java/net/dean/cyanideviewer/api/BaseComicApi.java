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

public abstract class BaseComicApi implements ComicApi {
	// Since getSavedImageDirectory() is called a lot, create one instance of the image directory
	// to speed things up, since it relies on a potentially expensive call (Environment.getExternalStorageDirectory())
	private static final File IMAGE_DIR = new File(Environment.getExternalStorageDirectory(), "CyanideViewer");
	private static final File ICON_DIR = new File(IMAGE_DIR, "icons");

	/** The HttpClient that will be used to host requests */
	protected HttpClient client;

	public BaseComicApi() {
		this.client = new DefaultHttpClient();
	}

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

	@Override
	public File getSavedImageDirectory() {
		return IMAGE_DIR;
	}

	@Override
	public File getSavedIconDirectory() {
		return ICON_DIR;
	}
}
