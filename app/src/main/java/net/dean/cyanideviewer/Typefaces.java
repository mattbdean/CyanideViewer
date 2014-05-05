package net.dean.cyanideviewer;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;

import java.util.Hashtable;

/**
 * https://code.google.com/p/android/issues/detail?id=9904#c7
 */
public class Typefaces {
	private static final Hashtable<String, Typeface> cache = new Hashtable<>();

	public static Typeface get(Context c, String assetPath) {
		synchronized (cache) {
			if (!cache.contains(assetPath)) {
				try {
					Typeface t = Typeface.createFromAsset(c.getAssets(), assetPath);
					cache.put(assetPath, t);
				} catch (Exception e) {
					Log.e(Constants.TAG, "Unable to get the typeface at " + assetPath, e);
					return null;
				}
			}
			return cache.get(assetPath);
		}
	}
}
