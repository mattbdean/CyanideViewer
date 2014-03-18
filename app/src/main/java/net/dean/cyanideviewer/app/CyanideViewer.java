package net.dean.cyanideviewer.app;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import net.dean.cyanideviewer.app.api.CyanideApi;

/**
 * Created by matthew on 3/16/14.
 */
public class CyanideViewer extends Application {
    public static final String TAG = "CyanideViewer";

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Starting application");
        CyanideViewer.context = getApplicationContext();
        new CyanideApi(); // Initialize CyanideApi.instance
    }

    public static Context getContext() {
        return context;
    }
}
