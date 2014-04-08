package net.dean.cyanideviewer.api;

import java.io.File;

public interface ComicApi {
	/** Gets the base URL from which to retrieve comics from */
	public String getBaseUrl();

	/** Gets the ID of the very first publicly available comic */
	public long getFirstId();

	/** Gets the ID of the newest publicly available comic */
	public long getNewestId();

	/** Gets the directory that this app will download comics to. Sample value: "/sdcard/CyanideViewer/" */
	public File getSavedImageDirectory();

	/**
	 * Parses an ID from a URL. For example, http://explosm.net/comics/1234 would return 1234.
	 * @param url The URL to parse
	 * @return The ID of the given comic URL
	 */
	public long getIdFromUrl(String url);

	/**
	 * Gets the URL of a Comic from the internet.
	 * @param id The ID of the comic to look up
	 * @return A String representing a URL, or null if the comic does not exist.
	 */
	public String getComicUrl(long id);

	/**
	 * Gets a Comic object based on the given ID. If the comic exists in the database, it will
	 * return the data stored in that row. If a row for that particular comic does not exist,
	 * one will be added. If a file
	 * @param id The ID of the comic to find
	 * @return A new Comic based on the current ID, or null if that comic does not exist.
	 */
	public Comic getComic(long id);

	/**
	 * Gets the comic relative to the of the comic given.
	 * @param relativeToId The ID of the comic to get the comic previous to
	 * @return The comic before the given ID
	 */
	public Comic getPrevious(long relativeToId);

	/**
	 * Gets the next comic relative to a certain ID
	 * @param relativeToId The ID to use
	 * @return The comic next in line from the given ID
	 */
	public Comic getNext(long relativeToId);

	/**
	 * Gets the newest comic
	 * @return The newest comic
	 */
	public Comic getNewest();

	/**
	 * Gets the first comic
	 * @return The first comic
	 */
	public Comic getFirst();

	/**
	 * Gets a random comic
	 * @return A random comic
	 */
	public Comic getRandom();
	public boolean getSupportsRandomComics();

	/**
	 * Tests if a comic with a certain ID has been downloaded
	 * @param id The ID to use
	 * @return Whether the comic has been downloaded
	 */
	public boolean hasLocalComic(long id);

	/**
	 * Gets the location of a comic on the filesystem.
	 * @param id The ID of the comic to find
	 * @return A File pointing to the comic represented by the given ID
	 */
	public File getLocalComic(long id);
}
