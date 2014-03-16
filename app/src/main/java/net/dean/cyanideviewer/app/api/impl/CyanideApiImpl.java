package net.dean.cyanideviewer.app.api.impl;

import android.graphics.Bitmap;
import android.widget.ImageView;
import android.widget.TextView;

import net.dean.cyanideviewer.app.api.CyanideApi;

/**
 * Created by matthew on 3/16/14.
 */
public class CyanideApiImpl {
    private ImageView imageView;
    private TextView label;

    public CyanideApiImpl(ImageView imageView, TextView label) {
        this.imageView = imageView;
        this.label = label;
    }

    public void setComicNewest() {
        new RetrieveComicTask(imageView, label, this).execute(CyanideApi.instance.getNewestId());
    }

    public void setComicFirst() {
        new RetrieveComicTask(imageView, label, this).execute(CyanideApi.instance.getFirstId());
    }

    public void setComicRandom() {
        new RetrieveRandomComicTask(imageView, label, this).execute();
    }

    public void setComicNext() {
        new AbstractComicTask<Long>(imageView, label, this) {

            @Override
            protected Bitmap doInBackground(Long... params) {
                return CyanideApi.instance.getNext(params[0]);
            }
        }.execute(CyanideApi.instance.currentId);
    }

    public void setComicPrevious() {
        new AbstractComicTask<Long>(imageView, label, this) {

            @Override
            protected Bitmap doInBackground(Long... params) {
                return CyanideApi.instance.getPrevious(params[0]);
            }
        }.execute(CyanideApi.instance.currentId);
    }
}
