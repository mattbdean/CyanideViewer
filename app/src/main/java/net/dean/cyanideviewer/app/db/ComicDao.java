package net.dean.cyanideviewer.app.db;

import net.dean.cyanideviewer.app.api.Comic;

import java.util.List;

/**
 * Created by matthew on 3/18/14.
 */
public interface ComicDao {
	public List<Comic> getAllComics();
	public List<Comic> getFavoriteComics();
	public Comic getComic(long id);
	public void addComic(Comic c);
	public void updateComicAsFavorite(Comic c);
	public void deleteComic(Comic c);
}
