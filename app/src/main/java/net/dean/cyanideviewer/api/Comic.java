package net.dean.cyanideviewer.api;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.dean.cyanideviewer.ComicStage;
import net.dean.cyanideviewer.CyanideUtils;
import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.FavoriteComicListItem;
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
	/** The width and height of all generated icons */
	public static final int ICON_DIMENS = 144;

	/** The ID of the comic. */
	private long id;

	/** The URL of the comic's image */
	private URL url;

	/**  Whether this comic is a favorite of the user's */
	private boolean isFavorite;

	/** The Bitmap that represents the comic */
	private Bitmap bitmap;

	/**
	 * Whether the comic has been fully loaded
	 */
	private boolean hasLoaded;

	private OnComplete onBitmapLoaded;

	/**
	 * Instantiates a new Comic assuming the comic is not a favorite
	 *
	 * @param id  The ID of the comic
	 * @param url The URL of the comic's image
	 */
	public Comic(long id, URL url) {
		this(id, url, false);
	}

	/**
	 * Instantiates a new Comic
	 *
	 * @param id         The ID of the comic
	 * @param url        The URL of the comic's image
	 * @param isFavorite If this comic is one of the user's favorites
	 */
	public Comic(long id, URL url, boolean isFavorite) {
		this.url = url;
		this.id = id;
		this.isFavorite = isFavorite;
		this.hasLoaded = false;
	}

	/**
	 * Instantiates a new Comic with a Parcel.
	 *
	 * @param in A Parcel created by {@link #CREATOR}
	 */
	public Comic(Parcel in) {
		this.id = in.readLong();
		this.url = CyanideUtils.newUrl(in.readString());
		this.isFavorite = in.readByte() != 0;
	}

	/**
	 * Loads and assigns the {@link #bitmap} attribute and then sets the source of a ComicStage's
	 * ImageView to it.
	 * @param target The ComicStage to use
	 */
	public void loadBitmap(ComicStage target) {
		new BitmapLoaderTask(target).execute();
	}

	/**
	 * Loads the icon of this comic to the ImageView of a FavoriteComicListItem
	 * @param item The FavoriteComicListItem to use
	 */
	public void loadIcon(FavoriteComicListItem item) {
		new IconLoaderTask(item).execute();
	}

	/**
	 * Copies the value of this comic's bitmap to a file called "<id>.<ext>"
	 * @param downloadButton The button to disable after downloading
	 */
	public void download(final ImageButton downloadButton) {
		download(bitmap, downloadButton);
	}

	/**
	 * Tries to download this comic to the local file system. Will download to
	 * "/sdcard/Cyanide Viewer/$id.$extension
	 *
	 * @param downloadButton The button to disable after downloading
	 */
	public void download(final Bitmap bitmap, final ImageButton downloadButton) {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected void onPreExecute() {
				Toast.makeText(CyanideViewer.getContext(), "Download starting", Toast.LENGTH_SHORT).show();
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				File dest = new File(CyanideApi.instance().getSavedImageDirectory(), generateFileName());
				return writeBitmap(bitmap, dest);
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

	/**
	 * Creates an icon of the comic with the dimensions of {@value #ICON_DIMENS}x{@value #ICON_DIMENS}
	 * and saves it to the icon directory provided by CyanideApi.getSavedImageDirectory()
	 */
	public void downloadIcon() {
		new AsyncTask<Void, Void, Boolean>() {

			@Override
			protected Boolean doInBackground(Void... params) {
				// http://stackoverflow.com/a/6909144/1275092
				Bitmap icon;

				if (bitmap.getWidth() >= bitmap.getHeight()) {
					icon = Bitmap.createBitmap(bitmap, bitmap.getWidth() / 2 - bitmap.getHeight() / 2, 0, ICON_DIMENS, ICON_DIMENS);
				} else {
					icon = Bitmap.createBitmap(bitmap, 0, bitmap.getHeight() / 2 - bitmap.getWidth() / 2, bitmap.getWidth(), bitmap.getWidth());
				}

				File dest = new File(CyanideApi.instance().getSavedIconDirectory(), generateFileName());
				return writeBitmap(icon, dest);
			}

			@Override
			protected void onPostExecute(Boolean success) {
				if (success) {
					Log.i(CyanideViewer.TAG, "Downloaded icon for #" + id);
				} else {
					Log.w(CyanideViewer.TAG, "Could not download icon for #" + id);
				}
			}
		}.execute();
	}

	/**
	 * Writes a Bitmap object to a file
	 * @param source The Bitmap to use
	 * @param dest The destination of the file
	 * @return Whether or not this action succeeded
	 */
	private boolean writeBitmap(Bitmap source, File dest) {
		ByteArrayOutputStream bos = null;
		FileOutputStream fos = null;

		String ext = dest.getName().substring(dest.getName().lastIndexOf('.'));

		try {
			// If the destination is a directory, add the name to it
			if (dest.isDirectory()) {
				dest = new File(dest, generateFileName());
			}
			// Create the appropriate directories
			if (!dest.getParentFile().isDirectory()) {
				if (!dest.getParentFile().mkdirs()) {
					Log.e(CyanideViewer.TAG, "Unable to create the parent directories for "
							+ dest.getAbsolutePath());
				}
			}

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
			source.compress(targetFormat, 100, bos);
			byte[] bitmapData = bos.toByteArray();

			// Write the data to the file
			fos = new FileOutputStream(dest);
			fos.write(bitmapData);

			Log.i(CyanideViewer.TAG, "Wrote bitmap to " + dest.getAbsolutePath());
			return true;
		} catch (IOException e) {
			Log.e(CyanideViewer.TAG, "Failed to write the bitmap to " + dest.getAbsolutePath(), e);
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

	/**
	 * Generates a file name for this comic. If the comic's ID was 1241, and the extension provided
	 * by the URL was ".png", then the file name would be "1241.png".
	 * @return A file name based on this comic's ID and extension
	 */
	public String generateFileName() {
		String urlString = url.toExternalForm();
		String ext = urlString.substring(urlString.lastIndexOf(".") + 1);
		return (id + "." + ext);
	}

	/**
	 * Gets the URL
	 */
	public URL getUrl() {
		return url;
	}

	/**
	 * Sets the URL
	 *
	 * @param url The new URL
	 */
	public void setUrl(URL url) {
		this.url = url;
	}

	/**
	 * Gets the URL
	 */
	public long getId() {
		return id;
	}

	/**
	 * Sets the ID
	 *
	 * @param id The new ID
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * Gets whether or not this comic is a favorite
	 */
	public boolean isFavorite() {
		return isFavorite;
	}

	/**
	 * Sets whether or not this comic is a favorite of the user's
	 *
	 * @param isFavorite If the comic is a favorite
	 */
	public void setFavorite(boolean isFavorite) {
		this.isFavorite = isFavorite;
	}

	public void setOnBitmapLoaded(OnComplete action) {
		// Execute it if it has already been loaded
		if (hasLoaded) {
			// Don't assign onBitmapLoaded because we want it null so it only executes once
			action.onComplete();
		} else {
			onBitmapLoaded = action;
		}
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

	/**
	 * The Creator used to create Comics using Parcels
	 */
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
		return "Comic {" +
				"id=" + id +
				", url='" + url.toExternalForm() + '\'' +
				", isFavorite=" + isFavorite +
				'}';
	}

	/**
	 * Represents the task of loading a Comic's URL into a Bitmap usable by an ImageView
	 */
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
			// Adapted from http://stackoverflow.com/a/6621552/1275092

			try {
				if (url.getProtocol().equals("file")) {
					// Local file, no need to make any HTTP requests
					return BitmapFactory.decodeFile(URLDecoder.decode(url.getPath(), "UTF-8"));
				}

				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet(url.toURI());
				HttpResponse response = client.execute(request);
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					Log.w(CyanideViewer.TAG, "Failed to fetch comic at " + url.toExternalForm());
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
				Log.e(CyanideViewer.TAG, "URISyntaxException: " + url, e);
				return null;
			} catch (IOException e) {
				Log.e(CyanideViewer.TAG, "IOException while trying to decode the image from URL " + url, e);
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

				hasLoaded = true;

				if (onBitmapLoaded != null) {
					onBitmapLoaded.onComplete();
					onBitmapLoaded = null;
				}
			}
		}
	}

	/**
	 * Loads a Bitmap from the "icons" directory provided by CyanideApi.getSavedIconDirectory()
	 */
	private class IconLoaderTask extends AsyncTask<Void, Void, Bitmap> {
		private FavoriteComicListItem item;

		public IconLoaderTask(FavoriteComicListItem item) {
			this.item = item;
		}

		@Override
		protected Bitmap doInBackground(Void... params) {
			File iconFile = new File(CyanideApi.instance().getSavedIconDirectory(), generateFileName());
			if (!iconFile.isFile()) {
				return null;
			}

			return BitmapFactory.decodeFile(iconFile.getAbsolutePath());
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			// Hide the progress bar
			item.findViewById(R.id.progress_bar).setVisibility(View.INVISIBLE);
			ImageView favoriteIcon = (ImageView) item.findViewById(R.id.favorite_icon);

			if (bitmap != null) {
				// Bitmap was found and loaded
				favoriteIcon.setImageBitmap(bitmap);
			} else {
				// There was a problem
				favoriteIcon.setImageResource(R.drawable.ic_action_error);
			}
		}
	}

	public static interface OnComplete {
		public void onComplete();
	}
}
