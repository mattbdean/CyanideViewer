package net.dean.cyanideviewer;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.dean.cyanideviewer.api.Comic;
import net.dean.cyanideviewer.api.CyanideApi;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Represents the main component of this application: where the comic is presented. It contains three
 * main parts: an ImageView to hold the image, a ProgressBar to tell the user when the comic is
 * still loading, and a TextView to show the user the ID of the comic they are viewing.
 *
 * Panning/Zooming adapted from http://stackoverflow.com/a/6650484/1275092
 */
public class ComicStage extends LinearLayout {

	/**
	 * Creates a new instance of a ComicStage and its comic to the one given.
	 * @param comicId The ID of the comic to use
	 * @return A new ComicStage
	 */
	public static ComicStage newInstance(long comicId) {
		LayoutInflater li = (LayoutInflater) CyanideViewer.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		ComicStage cs = (ComicStage) li.inflate(R.layout.comic_stage, null);
		Log.d(CyanideViewer.TAG, "Creating a new ComicStage for #" + comicId);
		cs.setComic(comicId);
		return cs;
	}

	/**
	 * The ImageView that will host the comic
	 */
	private ImageView imageView;

	/**
	 * Whether the comic has been fully loaded
	 */
	private boolean hasLoaded;

	/**
	 * The ID of the comic to load
	 */
	private long idToLoad;

	/**
	 * The Comic that is to be displayed on this stage
	 */
	private Comic comic;

	/**
	 * The PhotoViewAttacher that provides pinch/double tap zooming and panning functionality
	 * to the ImageView
	 */
	private PhotoViewAttacher photoViewAttacher;

	/** Instantiates a new ComicStage */
	public ComicStage(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.comic = null;
		this.idToLoad = -1;
	}

	/** Sets the comic of this ComicStage. Will adjust the TextView and ImageView accordingly. */
	public void setComic(long id) {
		this.idToLoad = id;
		((TextView) findViewById(R.id.comic_id)).setText("#" + idToLoad);
		hasLoaded = false;
		// Set the ImageView to be clear if it has been set before
		if (imageView != null)
			imageView.setImageResource(android.R.color.transparent);
	}

	/**
	 * Loads the comic through the use of a BitmapLoaderTask
	 */
	public void loadComic() {
		if (idToLoad != -1) {
			new AbstractComicTask<Long>() {

				@Override
				protected Comic doInBackground(Long... params) {
					return CyanideApi.instance().getComic(params[0]);
				}

				@Override
				protected void onPostExecute(Comic comic) {
					if (comic != null) {
						comic.loadBitmap(ComicStage.this);
						hasLoaded = true;
						ComicStage.this.comic = comic;
					}
				}
			}.execute(idToLoad);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		if (photoViewAttacher != null) photoViewAttacher.cleanup();
		photoViewAttacher = null;
		super.onDetachedFromWindow();
	}

	/**
	 * Gets the comic associated with this stage
	 * @return The comic
	 */
	public Comic getComic() {
		return comic;
	}

	/**
	 * Gets the comic ID to load
	 * @return The ID
	 */
	public long getComicIdToLoad() {
		return idToLoad;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ComicStage {");
		sb.append("idToLoad=").append(idToLoad);
		sb.append(", comic=").append(comic);
		sb.append(", imageView=").append(imageView);
		sb.append(", hasLoaded=").append(hasLoaded);
		sb.append(", photoViewAttacher=").append(photoViewAttacher);
		sb.append('}');
		return sb.toString();
	}


}
