package net.dean.cyanideviewer.api.comic;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.ImageButton;

import net.dean.cyanideviewer.NotificationHelper;
import net.dean.cyanideviewer.api.CyanideApi;

import java.io.File;
import java.util.ArrayList;

/**
 * This class is used to download bitmaps from the internet and show notifications about them
 */
class BitmapDownloadTask extends AsyncTask<Void, Void, Boolean> {
	/** The ID of the notification */
	private static final int DOWNLOAD_NOTIF_ID = 1;

	/** A list of downloaded comics */
	private static final ArrayList<Integer> DOWNLOADED_IDS = new ArrayList<>();

	/** The NotificationHelper that will help show notifications about the downloads */
	private NotificationHelper notifHelper;

	/** The Comic to download the bitmap from */
	private Comic c;

	/** The button to disable after downloading */
	private ImageButton downloadButton;

	/**
	 * Instantiates a new BitmapDownloadTask
	 * @param c The Comic to download
	 * @param downloadButton The button to disable after downloading
	 */
	public BitmapDownloadTask(Comic c, ImageButton downloadButton) {
		this.c = c;
		this.downloadButton = downloadButton;
		this.notifHelper = NotificationHelper.getInstance(downloadButton.getContext(), DOWNLOAD_NOTIF_ID);
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		File dest = new File(CyanideApi.instance().getSavedImageDirectory(), c.generateFileName());
		return c.writeBitmap(dest);
	}

	@Override
	protected void onPostExecute(Boolean success) {
		// TODO use 'success' variable

		Intent intent = new Intent(downloadButton.getContext(), NotificationReceiver.class);
		intent.putIntegerArrayListExtra("downloaded_ids", DOWNLOADED_IDS);

		PendingIntent resetDownloadIntent = PendingIntent.getBroadcast(downloadButton.getContext(),
				0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

		notifHelper.builder()
				.setContentTitle("Downloaded comics")
				.setDeleteIntent(resetDownloadIntent);

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

	/**
	 * This class receives notifications from when the notification is cleared and erases the contents
	 * of {@link #DOWNLOADED_IDS}
	 */
	public static class NotificationReceiver extends BroadcastReceiver {
		// Because this class is a receiver and declared in AndroidManifest.xml, it must be declared public.
		// Otherwise, it would be private

		@Override
		public void onReceive(Context context, Intent intent) {
			DOWNLOADED_IDS.clear();
		}
	}
}
