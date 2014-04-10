package net.dean.cyanideviewer.api.comic;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.api.CyanideApi;

import java.io.File;

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
		return c.writeBitmap(icon, dest);
	}

	@Override
	protected void onPostExecute(Boolean success) {
		if (success) {
			Log.i(CyanideViewer.TAG, "Downloaded icon for #" + c.getId());
		} else {
			Log.w(CyanideViewer.TAG, "Could not download icon for #" + c.getId());
		}
	}
}
