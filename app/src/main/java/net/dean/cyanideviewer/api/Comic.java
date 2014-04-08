package net.dean.cyanideviewer.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.dean.cyanideviewer.ComicStage;
import net.dean.cyanideviewer.CyanideUtils;
import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.R;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;

import uk.co.senab.photoview.PhotoViewAttacher;

/**
 * Represents an Cyanide and Happiness comic.
 */
public class Comic implements Parcelable {
	/** The ID of the comic. */
	private long id;

	/** The URL of the comic's image */
	private URL url;

	/** Whether this comic is a favorite of the user's */
	private boolean isFavorite;

	private Bitmap bitmap;

	/**
	 * Instantiates a new Comic assuming the comic is not a favorite
	 * @param id The ID of the comic
	 * @param url The URL of the comic's image
	 */
	public Comic(long id, URL url) {
		this(id, url, false);
	}

	/**
	 * Instantiates a new Comic
	 * @param id The ID of the comic
	 * @param url The URL of the comic's image
	 * @param isFavorite If this comic is one of the user's favorites
	 */
	public Comic(long id, URL url, boolean isFavorite) {
		this.url = url;
		this.id = id;
		this.isFavorite = isFavorite;
	}

	/**
	 * Instanties a new Comic with a Parcel.
	 * @param in A Parcel created by {@link #CREATOR}
	 */
	public Comic(Parcel in) {
		this.id = in.readLong();
		this.url = CyanideUtils.newUrl(in.readString());
		this.isFavorite = in.readByte() != 0;
	}

	public void loadBitmap(ComicStage target) {
		new BitmapLoaderTask(target).execute(id);
	}

	/**
	 * Tries to download this comic to the local file system. Will download to
	 * "/sdcard/Cyanide Viewer/$id.$extension
	 * @return Whether or not the download succeeded.
	 * @param downloadButton
	 */
	public void download(final ImageButton downloadButton) {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected void onPreExecute() {
				Toast.makeText(CyanideViewer.getContext(), "Download starting", Toast.LENGTH_SHORT).show();
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				ByteArrayOutputStream bos = null;
				FileOutputStream fos = null;

				String urlString = getUrl().toExternalForm();
				String ext = urlString.substring(urlString.lastIndexOf('.'));
				File dest = new File(CyanideApi.IMAGE_DIR, getId() + ext);

				try {
					// "/sdcard/CyanideViewer"
					if (!(CyanideApi.IMAGE_DIR.mkdirs() || CyanideApi.IMAGE_DIR.isDirectory())) {
						// The image dir is not a directory or there was an error creating the folder
						Log.e(CyanideViewer.TAG, "Error while creating " + CyanideApi.IMAGE_DIR.getAbsolutePath()
								+ ". Is it not a directory?");
					}
					if (!dest.exists()) {
						bos = new ByteArrayOutputStream();

						// Copy the contents of the Bitmap to a file
						// http://stackoverflow.com/a/7780289/1275092
						Bitmap.CompressFormat targetFormat;

						// Decide on the compression format
						if (ext.equalsIgnoreCase("jpeg") || ext.equalsIgnoreCase("jpg")) {
							targetFormat = Bitmap.CompressFormat.JPEG;
						} else if (ext.equalsIgnoreCase("webp")) {
							// You never know...
							targetFormat = Bitmap.CompressFormat.WEBP;
						} else {
							// Assume PNG, because most comics are
							targetFormat = Bitmap.CompressFormat.PNG;
						}

						// Compress the bitmap for best quality
						bitmap.compress(targetFormat, 100, bos);
						byte[] bitmapData = bos.toByteArray();

						// Write the data to the file
						fos = new FileOutputStream(dest);
						fos.write(bitmapData);
					}
					Log.i(CyanideViewer.TAG, "Downloaded comic #" + id + " to " + dest.getAbsolutePath());
					return true;
				} catch (IOException e) {
					Log.e(CyanideViewer.TAG, "Failed to download #" + id, e);
					return false;
				} finally {
					// Close the resources
					try {
						if (bos != null) {
							bos.close();
						}
						if (fos != null) {
							fos.close();
						}
					} catch (IOException e) {
						Log.e(CyanideViewer.TAG, "Unable to close either the ByteOutputStream or the FileOutputStream", e);
					}
				}
			}

			@Override
			protected void onPostExecute(Boolean success) {
				String toastText;
				if (success) {
					toastText = "Comic downloaded";
				} else {
					toastText = "Comic failed to download!";
				}

				Toast.makeText(CyanideViewer.getContext(), toastText, Toast.LENGTH_SHORT).show();
				downloadButton.setEnabled(false);
			}
		}.execute();
	}

	/** Gets the URL */
	public URL getUrl() {
		return url;
	}

	/**
	 * Sets the URL
	 * @param url The new URL
	 */
	public void setUrl(URL url) {
		this.url = url;
	}

	/** Gets the URL */
	public long getId() {
		return id;
	}

	/**
	 * Sets the ID
	 * @param id The new ID
	 */
	public void setId(long id) {
		this.id = id;
	}

	/** Gets whether or not this comic is a favorite */
	public boolean isFavorite() {
		return isFavorite;
	}

	/**
	 * Sets whether or not this comic is a favorite of the user's
	 * @param isFavorite If the comic is a favorite
	 */
	public void setFavorite(boolean isFavorite) {
		this.isFavorite = isFavorite;
	}

	@Override
	public int describeContents() {
		// I don't know what I'm doing here
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(url.toExternalForm());
		dest.writeByte((byte) (isFavorite ? 1 : 0));
	}

	/** The Creator used to create Comics using Parcels */
	public static final Creator CREATOR = new Creator() {

		@Override
		public Object createFromParcel(Parcel source) {
			return new Comic(source);
		}

		@Override
		public Object[] newArray(int size) {
			return new Comic[size];
		}
	};

	@Override
	public String toString() {
		return "Comic{" +
				"id=" + id +
				", url='" + url.toExternalForm() + '\'' +
				'}';
	}

	/** Represents the task of loading a Comic's URL into a Bitmap usable by an ImageView */
	private class BitmapLoaderTask extends AsyncTask<Long, Void, Bitmap> {
		private ComicStage stage;

		public BitmapLoaderTask(ComicStage stage) {
			this.stage = stage;
		}

		@Override
		protected void onPreExecute() {
			if (stage != null) {
				TextView tv = (TextView) stage.findViewById(R.id.comic_id);
				tv.setText("#" + stage.getComicIdToLoad());
			}
		}

		@Override
		protected Bitmap doInBackground(Long... params) {
			Comic c = CyanideApi.getComic(params[0]);

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
				Comic.this.bitmap = bitmap;
				ImageView imageView = (ImageView) stage.findViewById(R.id.image_view);
				imageView.setImageBitmap(bitmap);
				new PhotoViewAttacher(imageView);
			}
		}
	}
}
