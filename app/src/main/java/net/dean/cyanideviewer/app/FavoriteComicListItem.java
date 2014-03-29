package net.dean.cyanideviewer.app;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.dean.cyanideviewer.app.api.Comic;

/**
 * Created by matthew on 3/23/14.
 */
public class FavoriteComicListItem extends LinearLayout {
	public static FavoriteComicListItem newInstance(Comic comic) {
		LayoutInflater li = (LayoutInflater) CyanideViewer.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		FavoriteComicListItem item = (FavoriteComicListItem) li.inflate(R.layout.favorite_item, null);
		item.setComic(comic);
		return item;
	}

	private Comic comic;

	public FavoriteComicListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public void setComic(Comic comic) {
		this.comic = comic;
		((TextView) findViewById(R.id.comic_id)).setText("#" + comic.getId());
	}

	public Comic getComic() {
		return comic;
	}
}
