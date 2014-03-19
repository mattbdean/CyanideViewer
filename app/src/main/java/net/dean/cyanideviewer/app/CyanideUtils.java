package net.dean.cyanideviewer.app;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by matthew on 3/19/14.
 */
public final class CyanideUtils {
    private CyanideUtils() {
        // No instances
    }

    public static URL newUrl(String href) {
        try {
            return new URL(href);
        } catch (MalformedURLException e) {
            Log.e(CyanideViewer.TAG, "Malformed URL: " + href, e);
        }
        return null;
    }
}
