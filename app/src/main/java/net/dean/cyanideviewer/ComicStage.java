package net.dean.cyanideviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.dean.cyanideviewer.api.Comic;
import net.dean.cyanideviewer.api.CyanideApi;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLDecoder;

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
			// The comic ID has been set
			new BitmapLoaderTask().execute(idToLoad);
			hasLoaded = true;
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
		final StringBuffer sb = new StringBuffer("ComicStage{");
		sb.append("idToLoad=").append(idToLoad);
		sb.append(", comic=").append(comic);
		sb.append(", imageView=").append(imageView);
		sb.append(", hasLoaded=").append(hasLoaded);
		sb.append(", photoViewAttacher=").append(photoViewAttacher);
		sb.append('}');
		return sb.toString();
	}

	/** Represents the task of loading a Comic's URL into a Bitmap usable by an ImageView */
	public class BitmapLoaderTask extends AsyncTask<Long, Void, Bitmap> {

		@Override
		protected void onPreExecute() {
			TextView tv = (TextView) findViewById(R.id.comic_id);
			tv.setText("#" + idToLoad);
		}

		@Override
		protected Bitmap doInBackground(Long... params) {
			Comic c = CyanideApi.getComic(params[0]);
			comic = c;

			// Adapted from http://stackoverflow.com/a/6621552/1275092

			try {
				if (c.getUrl().getProtocol().equals("file")) {
					// Local file, no need to make any HTTP requests
					return BitmapFactory.decodeFile(URLDecoder.decode(c.getUrl().getPath(), "UTF-8"));
				}

				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet(c.getUrl().toURI());
				HttpResponse response = client.execute(request);
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					Log.w(CyanideViewer.TAG, "Failed to fetch comic at " + c.getUrl().toExternalForm());
					return null;
				}
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					InputStream is = null;
					try {
						is = entity.getContent();
						return BitmapFactory.decodeStream(is);
					} finally {
						if (is != null) {
							is.close();
						}
						entity.consumeContent();
					}
				}

			} catch (URISyntaxException e) {
				Log.e(CyanideViewer.TAG, "URISyntaxException: " + c.getUrl(), e);
				return null;
			} catch (IOException e) {
				Log.e(CyanideViewer.TAG, "IOException while trying to decode the image from URL " + c.getUrl(), e);
				return null;
			}

			return null;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (bitmap != null) {
				imageView = (ImageView) findViewById(R.id.image_view);
				imageView.setImageBitmap(bitmap);
				if (photoViewAttacher == null) {
					photoViewAttacher = new PhotoViewAttacher(imageView);
				} else {
					photoViewAttacher.update();
				}
//				ProgressBar pb = (ProgressBar) findViewById(R.id.progress_bar);
//				// Remove the progress bar once the image is done loading
//				RelativeLayout layout = (RelativeLayout) pb.getParent();
//				layout.removeView(pb);
			}
		}
	}
}
