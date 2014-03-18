package net.dean.cyanideviewer.app.api.impl;

import net.dean.cyanideviewer.app.api.Comic;
import net.dean.cyanideviewer.app.api.CyanideApi;

/**
 * Created by matthew on 3/15/14.
 */
public class RetrieveRandomComicTask extends AbstractComicTask<Void> {

    public RetrieveRandomComicTask() {
        super();
    }

    @Override
    protected Comic doInBackground(Void... params) {
        return CyanideApi.instance.getRandom();
    }
}
