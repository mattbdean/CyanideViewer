package net.dean.cyanideviewer.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.dean.cyanideviewer.AbstractComicTask;
import net.dean.cyanideviewer.Callback;
import net.dean.cyanideviewer.Constants;
import net.dean.cyanideviewer.CyanideUtils;
import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.R;
import net.dean.cyanideviewer.Typefaces;
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
	public static ComicStage newInstance(long comicId) {
		LayoutInflater li = (LayoutInflater) CyanideViewer.getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		ComicStage cs = (ComicStage) li.inflate(R.layout.comic_stage, null);
		Log.d(Constants.TAG, "Creating a new ComicStage for #" + comicId);
		cs.setComic(comicId);
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

	private boolean showing;

	private View infoPane;


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
		this.imageView = (ImageView) findViewById(R.id.image_view);
		// Set the ImageView to be clear if it has been set before
		imageView.setImageBitmap(null);

		findViewById(R.id.more_info).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggle();
			}
		});
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		Typeface fontAwesome = Typefaces.get(getContext(), "fonts/fontawesome.ttf");

		final Button facebook = (Button) findViewById(R.id.author_facebook);
		facebook.setTypeface(fontAwesome);

		final Button twitter = (Button) findViewById(R.id.author_twitter);
		twitter.setTypeface(fontAwesome);

		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.getId() == facebook.getId()) {
					String fb = comic.getAuthor().getFacebook();
					openUri("fb://profile/" + fb, "https://www.facebook.com/" + fb);
				} else if (v.getId() == twitter.getId()) {
					String twitterStr = comic.getAuthor().getTwitter();
					openUri("twitter://user?screen_name=" + twitterStr, "https://twitter.com/" + twitterStr);
				}
			}
		};

		twitter.setOnClickListener(listener);
		facebook.setOnClickListener(listener);
	}

	void openUri(String primary, String fallback) {
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(primary));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // http://stackoverflow.com/a/3689900/1275092
		try {
			getContext().startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Log.i(Constants.TAG, String.format("Primary URI (%s) failed, using backup (%s)", primary, fallback), e);
			intent.setData(Uri.parse(fallback));
			getContext().startActivity(intent);
		}
	}

	void toggle() {
		showing = !showing;

		if (infoPane == null) {
			infoPane = findViewById(R.id.info_panel);
		}

		int colorId = R.color.text_unfocused;
		if (showing) {
			colorId = R.color.text_focused;
		}

		((TextView) findViewById(R.id.comic_id)).setTextColor(getResources().getColor(colorId));
		infoPane.setVisibility(showing ? VISIBLE : GONE);
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

						// Set the rest of the metadata
						((TextView) findViewById(R.id.published)).setText(CyanideUtils.format(comic.getPublished()));
						((ImageView)findViewById(R.id.author_icon)).setImageBitmap(comic.getAuthor().getIcon());
						((TextView) findViewById(R.id.author_name)).setText(comic.getAuthor().getName());


						if (onBitmapLoaded != null) {
							comic.setOnBitmapLoaded(onBitmapLoaded);
						}
						comic.loadBitmap(imageView);
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
		return "ComicStage {" +
				"imageView=" + imageView +
				", idToLoad=" + idToLoad +
				", comic=" + comic +
				", showing=" + showing +
				", infoPane=" + infoPane +
				'}';
	}
}
