package net.dean.cyanideviewer.app.api.impl;

import android.os.AsyncTask;

import net.dean.cyanideviewer.app.api.Comic;

/**
 * Created by matthew on 3/15/14.
 */
public abstract class AbstractComicTask<T> extends AsyncTask<T, Void, Comic> {
    public AbstractComicTask() {

    }

    @Override
    protected void onPostExecute(Comic c) {
        super.onPostExecute(c);
    }
}
