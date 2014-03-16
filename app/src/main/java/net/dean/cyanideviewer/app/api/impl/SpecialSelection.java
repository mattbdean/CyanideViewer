package net.dean.cyanideviewer.app.api.impl;

import net.dean.cyanideviewer.app.api.CyanideApi;

/**
 * Created by matthew on 3/16/14.
 */
public enum SpecialSelection {
    FIRST("15"),
    NEWEST("new"),
    RANDOM("random");

    private String url;

    private SpecialSelection(String urlAppendage) {
        this.url = CyanideApi.BASE_URL + urlAppendage;
    }

    public String getUrl() {
        return url;
    }
}
