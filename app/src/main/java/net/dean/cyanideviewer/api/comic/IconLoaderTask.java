package net.dean.cyanideviewer.api.comic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.FavoriteComicListItem;
import net.dean.cyanideviewer.R;
import net.dean.cyanideviewer.api.CyanideApi;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Loads a Bitmap from the "icons" directory provided by CyanideApi.getSavedIconDirectory()
 */
class IconLoaderTask extends AsyncTask<Void, Void, Bitmap> {
	/** The comic to load the icon for */
	private Comic comic;
	/** The list item whose components will be changed when the comic's icon is loaded */
	private FavoriteComicListItem item;

	/**
	 * Instantiates a new IconLoaderTask
	 * @param item The item to use
	 * @param comic The comic to use
	 */
	public IconLoaderTask(FavoriteComicListItem item, Comic comic) {
		this.item = item;
		this.comic = comic;
	}

	@Override
	protected Bitmap doInBackground(Void... params) {
		File iconFile = new File(CyanideApi.instance().getSavedIconDirectory(), comic.generateFileName());
		if (!iconFile.isFile()) {
			return null;
		}

		String bitmapHash = comic.getIconHash();
		try {
			HashUtils.check(iconFile, bitmapHash);
		} catch (HashMismatchException e) {
			Log.e(CyanideViewer.TAG, String.format("Hash mismatch for comic #%s. Deleting file \"%s\".",
					comic.getId(), iconFile.getAbsolutePath()));
			if (!iconFile.delete()) {
				Log.e(CyanideViewer.TAG, "Could not delete file " + iconFile.getAbsolutePath());
			}
			return null;
		} catch (FileNotFoundException e) {
			Log.e(CyanideViewer.TAG, "Could not find local comic at " + iconFile.getAbsolutePath() +
					". This really shouldn't happen.", e);
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
