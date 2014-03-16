package net.dean.cyanideviewer.app;

import android.app.Application;
import android.util.Log;

import net.dean.cyanideviewer.app.api.CyanideApi;

/**
 * Created by matthew on 3/16/14.
 */
public class CyanideViewer extends Application {
    public static final String TAG = "CyanideViewer";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Starting application");
        new CyanideApi(); // Initialize CyanideApi.instance
    }
}
