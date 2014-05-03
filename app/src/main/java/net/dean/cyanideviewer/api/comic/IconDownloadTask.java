package net.dean.cyanideviewer.api.comic;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import net.dean.cyanideviewer.Constants;
import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.api.CyanideApi;

import java.io.File;
import java.io.FileNotFoundException;

class IconDownloadTask extends AsyncTask<Void, Void, Boolean> {
	/** The width and height of all generated icons */
	public static final int ICON_DIMENS = 144;

	private Comic c;

	public IconDownloadTask(Comic c) {
		this.c = c;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		// http://stackoverflow.com/a/6909144/1275092
		Bitmap icon;

		if (c.getBitmap().getWidth() >= c.getBitmap().getHeight()) {
			icon = Bitmap.createBitmap(c.getBitmap(), c.getBitmap().getWidth() / 2 - c.getBitmap().getHeight() / 2, 0, ICON_DIMENS, ICON_DIMENS);
		} else {
			icon = Bitmap.createBitmap(c.getBitmap(), 0, c.getBitmap().getHeight() / 2 - c.getBitmap().getWidth() / 2, c.getBitmap().getWidth(), c.getBitmap().getWidth());
		}

		File dest = new File(CyanideApi.instance().getSavedIconDirectory(), c.generateFileName());
		boolean success = Comic.writeBitmap(icon, dest);

		if (!success) {
			return false;
		}

		// Set the bitmap hash because it's been downloaded
		try {
			c.setIconHash(HashUtils.getChecksum(dest));
			CyanideViewer.getComicDao().update(c);
		} catch (FileNotFoundException e) {
			Log.e(Constants.TAG_API, "Could not find file " + dest + ". This really shouldn't happen.", e);
		}

		return true;
	}

	@Override
	protected void onPostExecute(Boolean success) {
		if (success) {
			Log.i(Constants.TAG_API, "Downloaded icon for #" + c.getId());
		} else {
			Log.w(Constants.TAG_API, "Could not download icon for #" + c.getId());
		}
	}
}
