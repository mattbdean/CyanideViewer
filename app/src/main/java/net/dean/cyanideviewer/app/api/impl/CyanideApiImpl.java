package net.dean.cyanideviewer.app.api.impl;

import android.util.Log;

import net.dean.cyanideviewer.app.CyanideViewer;
import net.dean.cyanideviewer.app.MainActivity;
import net.dean.cyanideviewer.app.api.Comic;
import net.dean.cyanideviewer.app.api.CyanideApi;

import java.util.concurrent.ExecutionException;

/**
 * Created by matthew on 3/16/14.
 */
public class CyanideApiImpl {
    private MainActivity activity;

    public CyanideApiImpl(MainActivity activity) {
        this.activity = activity;
    }

    public void setComicNewest() {
        //new RetrieveComicTask(activity).execute(CyanideApi.instance.getNewestId());
        activity.getPagerAdapter().addView(CyanideApi.instance.getNewest());
    }

    public Comic getComicPrevious(long currentId) {
        // Yay! Overly complicated code!
        try {
            return new AbstractComicTask<Long>() {

                @Override
                protected Comic doInBackground(Long... params) {
                    return CyanideApi.instance.getPrevious(params[0]);
                }
            }.execute(currentId).get();
        } catch (InterruptedException | ExecutionException e) {
            Log.e(CyanideViewer.TAG, "Exception while trying to get the comic relative to #" + currentId);
            return null;
        }
    }

    public void setComicFirst() {
        //new RetrieveComicTask(activity).execute(CyanideApi.instance.getFirstId());
    }

    public void setComicRandom() {
        new RetrieveRandomComicTask().execute();
    }

    public void setComicNext() {
//        new AbstractComicTask<Long>(activity) {
//
//            @Override
//            protected Bitmap doInBackground(Long... params) {
//                //return CyanideApi.instance.getNext(params[0]);
//                return null;
//            }
//        }.execute(CyanideApi.instance.currentId);
        activity.getPagerAdapter().addView(CyanideApi.instance.getNext(CyanideApi.instance.currentId));
    }

    public void setComicPrevious() {
        new AbstractComicTask<Long>() {

            @Override
            protected Comic doInBackground(Long... params) {
                //return CyanideApi.instance.getPrevious(params[0]);
                return null;
            }
        }.execute(CyanideApi.instance.currentId);
    }
}
