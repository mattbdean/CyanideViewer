package net.dean.cyanideviewer.api;

/**
 * Represents the "special" URLs available to the user.
 */
public enum SpecialSelection {
	/** The newest comic, once it's redirect has been followed */
	NEWEST("new"),
	/** A random comic, once it's redirect has been followed */
	RANDOM("random");

	/** This SpecialSelection's fully qualified URL */
	private final String url;

	private SpecialSelection(String urlAppendage) {
		this.url = CyanideApi.instance().getBaseUrl() + urlAppendage;
	}

	/**
	 * Gets the URL associated with this special selection
	 * @return The URL
	 */
	public String getUrl() {
		return url;
	}
}
