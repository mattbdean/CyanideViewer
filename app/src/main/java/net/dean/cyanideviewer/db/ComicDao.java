package net.dean.cyanideviewer.db;

import net.dean.cyanideviewer.api.Comic;

import java.util.List;

/**
 * The data access object used to manipulate comics in the database
 */
public interface ComicDao {
	/**
	 * Gets a list of all comics in the database
	 * A list of all comics in the database
	 */
	public List<Comic> getAllComics();

	/**
	 * Gets a list of the user's favorite comics
	 * @return A list of the user's favorite comics
	 */
	public List<Comic> getFavoriteComics();

	/**
	 * Gets a Comic with a given ID
	 * @param id The ID to find
	 * @return A Comic, if it exists in the database. Null if it does not.
	 */
	public Comic getComic(long id);

	/**
	 * Tests if a comic exists in the database with the given id
	 * @param id The id to test
	 * @return True if the comic exists in the database, false if else
	 */
	public boolean comicExists(long id);

	/**
	 * Adds a Comic to the database
	 * @param c The Comic to add
	 */
	public void addComic(Comic c);

	/**
	 * Updates the row in the database that represents this comic and changes the value of 'is_favorite'
	 * to the value of Comic.isFavorite()
	 * @param c The comic to use
	 */
	public void updateComicAsFavorite(Comic c);

	/**
	 * Deletes a comic from the database
	 * @param c The comic to use
	 */
	public void deleteComic(Comic c);
}
