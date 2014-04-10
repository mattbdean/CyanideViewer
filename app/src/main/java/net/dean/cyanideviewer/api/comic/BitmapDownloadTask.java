package net.dean.cyanideviewer.api.comic;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageButton;

import net.dean.cyanideviewer.NotificationHelper;
import net.dean.cyanideviewer.api.CyanideApi;

import java.io.File;
import java.util.ArrayList;

class BitmapDownloadTask extends AsyncTask<Void, Void, Boolean> {
	private static final int DOWNLOAD_NOTIF_ID = 1;

	private static final ArrayList<Integer> DOWNLOADED_IDS = new ArrayList<>();

	private NotificationHelper notifHelper;

	private Comic c;
	private Bitmap bitmap;
	private ImageButton downloadButton;

	public BitmapDownloadTask(Comic c, Bitmap bitmap, ImageButton downloadButton) {
		this.c = c;
		this.bitmap = bitmap;
		this.downloadButton = downloadButton;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		File dest = new File(CyanideApi.instance().getSavedImageDirectory(), c.generateFileName());
		return c.writeBitmap(bitmap, dest);
	}

	@Override
	protected void onPostExecute(Boolean success) {
		// TODO use 'success' variable
		if (notifHelper == null) {
			// Lazy initialize the helper since not all Comic objects will use this, since
			// not all comics will be downloaded

			notifHelper = NotificationHelper.getInstance(downloadButton.getContext(), DOWNLOAD_NOTIF_ID);

			Intent intent = new Intent(downloadButton.getContext(), NotificationReceiver.class);
			intent.putIntegerArrayListExtra("downloaded_ids", DOWNLOADED_IDS);

			PendingIntent resetDownloadIntent = PendingIntent.getBroadcast(downloadButton.getContext(),
					0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

			notifHelper.builder()
					.setContentTitle("Downloaded comics")
					.setDeleteIntent(resetDownloadIntent);
		}
		DOWNLOADED_IDS.add((int) c.getId());

		String content = "#" + c.getId();
		if (DOWNLOADED_IDS.size() > 1) {
			content += " and " + (DOWNLOADED_IDS.size() - 1) + " other";
		}
		if (DOWNLOADED_IDS.size() > 2) {
			// Make 'other' plural
			content += "s";
		}

		notifHelper.builder().setNumber(DOWNLOADED_IDS.size())
				.setContentText(content)
				.setTicker("Download comic #" + c.getId());
		notifHelper.notifyManager(); // Show the notification

		downloadButton.setEnabled(false);
	}


	public static class NotificationReceiver extends BroadcastReceiver {
		// Because this class is a receiver and declared in AndroidManifest.xml, it must be declared public.
		// Otherwise, it would be private

		@Override
		public void onReceive(Context context, Intent intent) {
			DOWNLOADED_IDS.clear();
		}
	}
}
