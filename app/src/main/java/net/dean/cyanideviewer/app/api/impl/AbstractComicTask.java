package net.dean.cyanideviewer.app.api.impl;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.TextView;

import net.dean.cyanideviewer.app.api.CyanideApi;

/**
 * Created by matthew on 3/15/14.
 */
public abstract class AbstractComicTask<T> extends AsyncTask<T, Void, Bitmap> {
    protected ImageView imageView;
    protected TextView label;
    protected CyanideApiImpl api;

    public AbstractComicTask(ImageView imageView, TextView label, CyanideApiImpl api) {
        this.imageView = imageView;
        this.label = label;
        this.api = api;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        if (bitmap != null) {
            this.imageView.setImageBitmap(bitmap);
            this.label.setText("#" + CyanideApi.instance.currentId);
        }
    }
}
