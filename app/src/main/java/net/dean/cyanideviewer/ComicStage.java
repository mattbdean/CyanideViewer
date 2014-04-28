package net.dean.cyanideviewer;

import android.app.FragmentManager;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.dean.cyanideviewer.api.CyanideApi;
import net.dean.cyanideviewer.api.comic.Comic;

/**
 * Represents the main component of this application: where the comic is presented. It contains three
 * main parts: an ImageView to hold the image, a ProgressBar to tell the user when the comic is
 * still loading, and a TextView to show the user the ID of the comic they are viewing.
 *
 */
public class ComicStage extends LinearLayout {

	/**
	 * Creates a new instance of a ComicStage and its comic to the one given.
	 * @param comicId The ID of the comic to use
	 * @return A new ComicStage
	 */
	public static ComicStage newInstance(long comicId, FragmentManager fragmentManager) {
		LayoutInflater li = (LayoutInflater) CyanideViewer.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		ComicStage cs = (ComicStage) li.inflate(R.layout.comic_stage, null);
		Log.d(CyanideViewer.TAG, "Creating a new ComicStage for #" + comicId);
		cs.setComic(comicId, fragmentManager);
		return cs;
	}

	/**
	 * The ImageView that will host the comic
	 */
	private ImageView imageView;


	/**
	 * The ID of the comic to load
	 */
	private long idToLoad;

	/**
	 * The Comic that is to be displayed on this stage
	 */
	private Comic comic;


	/** Instantiates a new ComicStage */
	public ComicStage(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.comic = null;
		this.idToLoad = -1;
	}

	/** Sets the comic of this ComicStage. Will adjust the TextView and ImageView accordingly. */
	public void setComic(long id, final FragmentManager fragmentManager) {
		this.idToLoad = id;
		((TextView) findViewById(R.id.comic_id)).setText("#" + idToLoad);
		this.imageView = (ImageView) findViewById(R.id.image_view);
		// Set the ImageView to be clear if it has been set before
		imageView.setImageResource(android.R.color.transparent);

		findViewById(R.id.author_button).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new AuthorDialog(comic.getAuthor()).show(fragmentManager, "authorDialog");
			}
		});
	}

	/**
	 * Loads the comic through the use of a BitmapLoaderTask
	 */
	public void loadComic() {
		loadComic(null);
	}

	/**
	 * Loads the comic through the use of a BitmapLoaderTask
	 * @param onBitmapLoaded Executed when the bitmap is finished loading
	 */
	public void loadComic(final Callback<Void> onBitmapLoaded) {
		if (idToLoad != -1) {
			new AbstractComicTask<Long>() {

				@Override
				protected Comic doInBackground(Long... params) {
					return CyanideApi.instance().getComic(params[0]);
				}

				@Override
				protected void onPostExecute(Comic comic) {
					if (comic != null) {
						ComicStage.this.comic = comic;
						if (onBitmapLoaded != null) {
							comic.setOnBitmapLoaded(onBitmapLoaded);
						}
						comic.loadBitmap(ComicStage.this);
					}
				}
			}.execute(idToLoad);
		}
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

	public ImageView getImageView() {
		return imageView;
	}


	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("ComicStage {");
		sb.append("idToLoad=").append(idToLoad);
		sb.append(", comic=").append(comic);
		sb.append(", imageView=").append(imageView);
		sb.append('}');
		return sb.toString();
	}
}
