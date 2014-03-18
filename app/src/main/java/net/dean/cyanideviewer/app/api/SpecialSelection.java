package net.dean.cyanideviewer.app.api;

/**
 * Represents the "special" URLs available to the user.
 */
public enum SpecialSelection {
    /** The first comic */
    FIRST("15"),
    /** The newest comic, once it's redirect has been followed */
    NEWEST("new"),
    /** A random comic, once it's redirect has been followed */
    RANDOM("random");

    /** This SpecialSelection's fully qualified URL */
    private String url;

    private SpecialSelection(String urlAppendage) {
        this.url = CyanideApi.BASE_URL + urlAppendage;
    }

    /**
     * Gets the URL associated with this special selection
     * @return The URL
     */
    public String getUrl() {
        return url;
    }
}
