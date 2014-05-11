package net.dean.cyanideviewer.api.comic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import net.dean.cyanideviewer.Constants;

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

	private final Comic comic;
	private final ImageView imageView;

	public BitmapLoaderTask(Comic comic, ImageView imageView) {
		this.comic = comic;
		this.imageView = imageView;
	}

	@Override
	protected Bitmap doInBackground(Long... params) {
		// Adapted from http://stackoverflow.com/a/6621552/1275092

		try {
			if (comic.getUrl().getProtocol().equals("file")) {
				// Local file, no need to make any HTTP requests
				return BitmapFactory.decodeFile(URLDecoder.decode(comic.getUrl().getPath(), "UTF-8"));
			}

			HttpClient client = new DefaultHttpClient();
			client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, false);
			HttpGet request = new HttpGet(comic.getUrl().toURI());
			HttpResponse response = client.execute(request);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				Log.w(Constants.TAG_API, "Failed to fetch comic at " + comic.getUrl().toExternalForm());
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
			Log.e(Constants.TAG_API, "URISyntaxException: " + comic.getUrl(), e);
			return null;
		} catch (IOException e) {
			Log.e(Constants.TAG_API, "IOException while trying to decode the image from URL " + comic.getUrl(), e);
			return null;
		}

		return null;
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
		if (bitmap != null) {
			comic.setBitmap(bitmap);

			imageView.setImageBitmap(bitmap);
			PhotoViewAttacher attacher = new PhotoViewAttacher(imageView);
			attacher.setMediumScale(1.5F); // Default of 1.75
			attacher.setMaximumScale(2.0F); // Default of 3

			comic.setHasLoaded(true);

			if (comic.getOnBitmapLoaded() != null) {
				comic.getOnBitmapLoaded().onComplete(null);
				comic.resetOnBitmapLoaded();
			}
		}
	}
}
