package net.dean.cyanideviewer.api.comic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import net.dean.cyanideviewer.ComicStage;
import net.dean.cyanideviewer.Constants;
import net.dean.cyanideviewer.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URLDecoder;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Represents the task of loading a Comic's URL into a Bitmap usable by an ImageView
 */
class BitmapLoaderTask extends AsyncTask<Long, Void, Bitmap> {
	/**
	 * The ComicStage whose components will be altered after the bitmap and it's metadata have finished
	 * loading
	 */
	private ComicStage stage;

	/**
	 * Instantiates a new BitmapLoaderTask
	 * @param stage The ComicStage to use
	 */
	public BitmapLoaderTask(ComicStage stage) {
		this.stage = stage;
	}

	@Override
	protected void onPreExecute() {
		((TextView) stage.findViewById(R.id.comic_id)).setText("#" + stage.getComic().getId());
		((ImageButton) stage.findViewById(R.id.author_button)).setImageResource(stage.getComic().getAuthor().getIconResource());
		((TextView) stage.findViewById(R.id.comic_date_published)).setText(stage.getComic().getPublishedFormatted());
	}

	@Override
	protected Bitmap doInBackground(Long... params) {
		// Adapted from http://stackoverflow.com/a/6621552/1275092

		try {
			if (stage.getComic().getUrl().getProtocol().equals("file")) {
				// Local file, no need to make any HTTP requests
				return BitmapFactory.decodeFile(URLDecoder.decode(stage.getComic().getUrl().getPath(), "UTF-8"));
			}

			HttpClient client = new DefaultHttpClient();
			client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, false);
			HttpGet request = new HttpGet(stage.getComic().getUrl().toURI());
			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				Log.w(Constants.TAG, "Failed to fetch comic at " + stage.getComic().getUrl().toExternalForm());
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
			Log.e(Constants.TAG, "URISyntaxException: " + stage.getComic().getUrl(), e);
			return null;
		} catch (IOException e) {
			Log.e(Constants.TAG, "IOException while trying to decode the image from URL " + stage.getComic().getUrl(), e);
			return null;
		}

		return null;
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
		if (bitmap != null) {
			stage.getComic().setBitmap(bitmap);

			ImageView imageView = stage.getImageView();
			imageView.setImageBitmap(bitmap);
			new PhotoViewAttacher(imageView);

			stage.getComic().setHasLoaded(true);

			if (stage.getComic().getOnBitmapLoaded() != null) {
				stage.getComic().getOnBitmapLoaded().onComplete(null);
				stage.getComic().resetOnBitmapLoaded();
			}
		}
	}
}
