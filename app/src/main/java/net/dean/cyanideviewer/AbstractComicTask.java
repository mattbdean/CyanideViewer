package net.dean.cyanideviewer;

import android.os.AsyncTask;

import net.dean.cyanideviewer.api.comic.Comic;

/**
 * Represents an background process to get a comic (whether that be from a URL or a file)
 * @param <T> The parameters of AsyncTask.execute()
 */
public abstract class AbstractComicTask<T> extends AsyncTask<T, Void, Comic> {

}
