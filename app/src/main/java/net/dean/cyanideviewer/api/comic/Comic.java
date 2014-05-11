package net.dean.cyanideviewer.api.comic;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;

import net.dean.cyanideviewer.Callback;
import net.dean.cyanideviewer.Constants;
import net.dean.cyanideviewer.CyanideUtils;
import net.dean.cyanideviewer.ui.FavoriteComicListItem;
import net.dean.cyanideviewer.db.DatabaseField;
import net.dean.cyanideviewer.db.Model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Represents an Cyanide and Happiness comic.
 */
public class Comic extends Model implements Parcelable {
	private static final DateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US);

	/** The URL of the comic's image */
	@DatabaseField(columnName = "url")
	private URL url;

	// Must be Boolean (capital 'B') because it must be an object for the DatabaseField
	// class to work properly
	/**  Whether this comic is a favorite of the user's */
	@DatabaseField(columnName = "is_favorite", doesUpdate = true)
	private Boolean isFavorite;

	/** The date this comic was published */
	@DatabaseField(columnName = "published")
	private Date published;

	/** The author who published this comic */
	@DatabaseField(columnName = "author_id")
	private Author author;

	/** The MD5 hash of the bitmap */
	@DatabaseField(columnName = "bitmap_hash", doesUpdate = true)
	private String bitmapHash;

	/** The MD5 hash of the comic's icon */
	@DatabaseField(columnName = "icon_hash", doesUpdate = true)
	private String iconHash;

	/** The Bitmap that represents the comic */
	private Bitmap bitmap;

	/** Whether the comic has been fully loaded */
	private boolean hasLoaded;

	/** Called when the bitmap has finished loading */
	private Callback<Void> onBitmapLoaded;

	/**
	 * Instantiates a new Comic assuming the comic is not a favorite
	 *
	 * @param id  The ID of the comic
	 * @param url The URL of the comic's image
	 * @param published The date the comic was published
	 * @param author The author of the comic
	 */
	public Comic(long id, URL url, Date published, Author author) {
		this(id, url, published, author, false);
	}


	/**
	 * Instantiates a new Comic
	 * @param id The ID of the comic
	 * @param url The URL of the comic's image
	 * @param published The date the comic was published
	 * @param author The author of the comic
	 * @param isFavorite If the comic is a favorite
	 */
	public Comic(long id, URL url, Date published, Author author, boolean isFavorite) {
		super(id);
		this.url = url;
		this.published = published;
		this.author = author;
		this.isFavorite = isFavorite;
		this.hasLoaded = false;
	}

	/**
	 * Instantiates a new Comic with a Parcel.
	 *
	 * @param in A Parcel created by {@link #CREATOR}
	 */
	public Comic(Parcel in) {
		super(in.readLong());
		this.url = CyanideUtils.newUrl(in.readString());
		this.isFavorite = in.readByte() != 0;
		this.published = new Date(in.readLong());
		this.author = in.readParcelable(Author.class.getClassLoader());
		this.hasLoaded = false;
	}

	/**
	 * Loads and assigns the {@link #bitmap} attribute and then sets the source of a ComicStage's
	 * ImageView to it.
	 * @param imageView The ImageView to assign the loaded bitmap to
	 */
	public void loadBitmap(ImageView imageView) {
		new BitmapLoaderTask(this, imageView).execute();
	}

	/**
	 * Loads the icon of this comic to the ImageView of a FavoriteComicListItem
	 * @param item The FavoriteComicListItem to use
	 */
	public void loadIcon(FavoriteComicListItem item) {
		new IconLoaderTask(item, this).execute();
	}

	/**
	 * Tries to download this comic to the local file system. Will download to
	 * "/sdcard/Cyanide Viewer/$id.$extension
	 *
	 * @param downloadButton The button to disable after downloading
	 */
	public void download(final ImageButton downloadButton) {
		new BitmapDownloadTask(this, downloadButton).execute();
	}

	/**
	 * Creates an icon of the comic with the dimensions of
	 * {@value net.dean.cyanideviewer.api.comic.IconDownloadTask#ICON_DIMENS} by
	 * {@value net.dean.cyanideviewer.api.comic.IconDownloadTask#ICON_DIMENS} pixels
	 * and saves it to the icon directory provided by CyanideApi.getSavedImageDirectory()
	 */
	public void downloadIcon() {
		new IconDownloadTask(this).execute();
	}

	/**
	 * Writes a Bitmap object to a file
	 * @param dest The destination of the file
	 * @return Whether or not this action succeeded
	 */
	boolean writeBitmap(File dest) {
		// If the destination is a directory, add the name to it
		if (dest.isDirectory()) {
			dest = new File(dest, generateFileName());
		}
		// Create the appropriate directories
		if (!dest.getParentFile().isDirectory()) {
			if (!dest.getParentFile().mkdirs()) {
				Log.e(Constants.TAG_API, "Unable to create the parent directories for "
						+ dest.getAbsolutePath());
			}
		}

		return writeBitmap(bitmap, dest);
	}

	/**
	 * Writes a Bitmap object to a file
	 * @param source The Bitmap to write
	 * @param dest The destination of the file
	 * @return Whether or not this action succeeded
	 */
	static boolean writeBitmap(Bitmap source, File dest) {
		ByteArrayOutputStream bos = null;
		FileOutputStream fos = null;

		String ext = dest.getName().substring(dest.getName().lastIndexOf('.'));
		// Create the parent file if it doesn't exist
		if (!dest.getParentFile().isDirectory()) {
			if (!dest.getParentFile().mkdirs()) {
				Log.e(Constants.TAG_API, "Could not create the directory \"" + dest.getAbsolutePath() + "\"");
			}
		}

		try {
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

			Log.i(Constants.TAG_API, "Wrote bitmap to " + dest.getAbsolutePath());
			return true;
		} catch (IOException e) {
			Log.e(Constants.TAG_API, "Failed to write the bitmap to " + dest.getAbsolutePath(), e);
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
				Log.e(Constants.TAG_API, "Unable to close either the ByteOutputStream or the FileOutputStream", e);
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

	/**
	 * Sets the bitmap
	 * @param bitmap The new bitmap
	 */
	void setBitmap(Bitmap bitmap) {
		this.bitmap = bitmap;
	}

	/**
	 * Gets the bitmap
	 * @return This comic's bitmap
	 */
	public Bitmap getBitmap() {
		return bitmap;
	}

	/**
	 * Sets if this comic's bitmap has loaded yet
	 * @param hasLoaded If the comic has loaded yet
	 */
	void setHasLoaded(boolean hasLoaded) {
		this.hasLoaded = hasLoaded;
	}

	/**
	 * Sets the action to take place once the bitmap has loaded. If it has already loaded, the action
	 * is called immediately.
	 * @param action The action to do
	 */
	public void setOnBitmapLoaded(Callback<Void> action) {
		// Execute it if it has already been loaded
		if (hasLoaded) {
			// Don't assign onBitmapLoaded because we want it null so it only executes once
			action.onComplete(null);
		} else {
			onBitmapLoaded = action;
		}
	}

	public Date getPublished() {
		return published;
	}

	public String getPublishedFormatted() {
		return dateFormat.format(published);
	}

	public void setPublished(Date published) {
		this.published = published;
	}

	public Author getAuthor() {
		return author;
	}

	public void setAuthor(Author author) {
		this.author = author;
	}

	public String getBitmapHash() {
		return bitmapHash;
	}

	public void setBitmapHash(String bitmapHash) {
		this.bitmapHash = bitmapHash;
	}

	public String getIconHash() {
		return iconHash;
	}

	public void setIconHash(String iconHash) {
		this.iconHash = iconHash;
	}

	/**
	 * Gets the Callback to execute when the bitmap has finished loading
	 */
	public Callback<Void> getOnBitmapLoaded() {
		return onBitmapLoaded;
	}

	/**
	 * Resets the onBitmapLoaded field
	 */
	void resetOnBitmapLoaded() {
		onBitmapLoaded = null;
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
				"url=" + url +
				", isFavorite=" + isFavorite +
				", published=" + published +
				", author=" + author +
				", bitmap=" + bitmap +
				", hasLoaded=" + hasLoaded +
				", bitmapHash='" + bitmapHash + '\'' +
				", iconHash=" + iconHash +
				'}';
	}
}
