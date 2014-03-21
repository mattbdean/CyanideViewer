package net.dean.cyanideviewer.app.api;

import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import net.dean.cyanideviewer.app.CyanideViewer;
import net.dean.cyanideviewer.app.MainActivity;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents an Cyanide and Happiness comic.
 */
public class Comic implements Parcelable {
	/** The ID of the comic. */
	private long id;

	/** The URL of the comic's image */
	private URL url;

	private boolean isFavorite;

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
		in.readBundle();
		this.id = in.readLong();
		String urlString = in.readString();
		try {
			this.url = new URL(urlString);
		} catch (MalformedURLException e) {
			Log.e(CyanideViewer.TAG, "Malformed URL while creating Parcelable Comic: " + urlString, e);
		}

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

	public boolean isFavorite() {
		return isFavorite;
	}

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

	/**
	 * Tries to download this comic to the local file system. Will download to
	 * "/sdard/Cyanide Viewer/$id.$extension
	 * @return Whether or not the download succeeded.
	 */
	public void download(final MainActivity mainActivity) {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					InputStream input = getUrl().openStream();

					// "/sdcard/CyanideViewer"
					CyanideApi.IMAGE_DIR.mkdirs();
						String urlString = getUrl().toExternalForm();
					String ext = urlString.substring(urlString.lastIndexOf('.'));
					File dest = new File(CyanideApi.IMAGE_DIR, getId() + ext);
					if (!dest.exists()) {
						// Only download it if the file doesn't already exist
						FileUtils.copyInputStreamToFile(input, dest);
					}
					Log.i(CyanideViewer.TAG, "Downloaded comic #" + id + " to " + dest.getAbsolutePath());
					return true;
				} catch (IOException e) {
					Log.e(CyanideViewer.TAG, "Failed to download #" + id, e);
					return false;
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
				mainActivity.refreshDownloadButtonState();
			}
		}.execute();
	}
}
