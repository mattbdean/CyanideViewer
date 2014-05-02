package net.dean.cyanideviewer;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * A collection of utility methods commonly used throughout the application
 */
public final class CyanideUtils {
	private CyanideUtils() {
		// No instances
	}

	/**
	 * Instantiates a new URL, handling the MalformedURLException that java.net.URL's constructor
	 * throws by logging it through Log.e()
	 * @param href The String representation of the URL
	 * @return A new URL if it is not malformed, null if it is malformed
	 */
	public static URL newUrl(String href) {
		try {
			return new URL(href);
		} catch (MalformedURLException e) {
			Log.e(Constants.TAG, "Malformed URL: " + href, e);
			return null;
		}
	}
}
