package net.dean.cyanideviewer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.dean.cyanideviewer.api.Comic;

/**
 * Represents an item found in the ListView in FavoritesActivity
 */
public class FavoriteComicListItem extends LinearLayout {
	/**
	 * Creates a new instance of FavoriteComicList item and sets the comic to the given one
	 * @param comic The Comic to use
	 * @return A new FavoriteComicListItem
	 */
	public static FavoriteComicListItem newInstance(Comic comic) {
		LayoutInflater li = (LayoutInflater) CyanideViewer.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		FavoriteComicListItem item = (FavoriteComicListItem) li.inflate(R.layout.favorite_item, null);
		item.setComic(comic);
		item.getComic().loadIcon(item);
		return item;
	}

	/**
	 * The Comic this list item represents
	 */
	private Comic comic;

	/**
	 * Instantiates a new FavoriteComicListItem
	 * @param context The context to use
	 * @param attrs The attribute set to use
	 */
	public FavoriteComicListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Sets the comic of this item
	 * @param comic The comic to use
	 */
	public void setComic(Comic comic) {
		this.comic = comic;
		((TextView) findViewById(R.id.comic_id)).setText("#" + comic.getId());
	}

	/**
	 * Gets the comic represented by this item
	 * @return The comic
	 */
	public Comic getComic() {
		return comic;
	}
}
