package net.dean.cyanideviewer.app.api.impl;

import net.dean.cyanideviewer.app.api.Comic;
import net.dean.cyanideviewer.app.api.CyanideApi;

/**
 * Created by matthew on 3/15/14.
 */
public class RetrieveComicTask extends AbstractComicTask<Long> {

    public RetrieveComicTask() {
        super();
    }

    @Override
    protected Comic doInBackground(Long... params) {
        return CyanideApi.instance.getComic(params[0]);
    }
}
