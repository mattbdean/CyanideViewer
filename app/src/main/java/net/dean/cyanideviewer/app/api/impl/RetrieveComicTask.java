package net.dean.cyanideviewer.app.api.impl;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import net.dean.cyanideviewer.app.api.CyanideApi;

/**
 * Created by matthew on 3/15/14.
 */
public class RetrieveComicTask extends AbstractComicTask<Long> {

    public RetrieveComicTask(ImageView imageView, TextView label, CyanideApiImpl api) {
        super(imageView, label, api);
    }

    @Override
    protected Bitmap doInBackground(Long... params) {
        return CyanideApi.instance.getComic(params[0]);
    }
}
