package net.dean.cyanideviewer.ui;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import net.dean.cyanideviewer.api.comic.Comic;

import java.util.ArrayList;

/**
 * A PagerAdapter to hold an infinite amount of ComicStages, given enough memory. Adapted from
 * http://stackoverflow.com/a/13671777/1275092
 */
public class ComicPagerAdapter extends PagerAdapter {
	/** A list of ComicStages that this adapter holds. */
	private final ArrayList<ComicStage> views = new ArrayList<>();

	@Override
	public int getItemPosition(Object object) {
		// Prevent suspicious call to indexOf()
		if (!(object instanceof ComicStage)) {
			return POSITION_NONE;
		}

		int index = views.indexOf(object);
		if (index == -1) {
			return POSITION_NONE;
		} else {
			return index;
		}
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position) {
		View v = views.get(position);
		container.addView(v);
		return v;
	}

	@Override
	public void destroyItem(ViewGroup container, int position, Object object) {
		container.removeView(views.get(position));
	}

	@Override
	public int getCount() {
		return views.size();
	}

	@Override
	public boolean isViewFromObject(View view, Object object) {
		return view == object;
	}

	/**
	 * Instantiates a new ComicStage with a given Comic and adds it to the list of views.
	 * @param c The comic to use to create a new ComicStage with
	 * @return The position at which this view was added
	 */
	public int addView(Comic c) {
		return addView(c, views.size());
	}

	/**
	 * Instantiates a new ComicStage with a given Comic and adds it to the list of views.
	 * @param c The comic to use to create a new ComicStage with
	 * @param position The position at which to insert the new View at
	 * @return The position at which this view was added
	 */
	public int addView(Comic c, int position) {
		views.add(position, ComicStage.newInstance(c.getId()));
		notifyDataSetChanged();
		return position;
	}

	/**
	 * Gets a ComicStage at a given position
	 * @param position The position at which to get the ComicStage
	 * @return A ComicStage, if the position exists, or null if it does not
	 */
	public ComicStage getComicStage(int position) {
		if (position >= 0 && position < views.size()) {
			return views.get(position);
		}
		return null;

	}
}