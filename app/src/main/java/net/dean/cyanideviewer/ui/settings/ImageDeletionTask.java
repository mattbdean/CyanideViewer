package net.dean.cyanideviewer.ui.settings;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import net.dean.cyanideviewer.Constants;
import net.dean.cyanideviewer.ui.NotificationHelper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for deleting image files (*.png, *.jpeg, *.jpg, *.gif) from a certain
 * directory
 */
class ImageDeletionTask extends AsyncTask<Void, Number, Number[]> {
	/**
	 * The template used to format the content text of the notification. It contains three
	 * %s tags. The first represents the amount of files that have been deleted. The second
	 * represents whether or not "files" should be plural. The third represents how many bytes
	 * (in a human readable format) have been deleted.
	 */
	private static final String PROGRESS_TEMPLATE = "Deleted %s file%s (%s)";

	/** The extensions to search and destroy */
	private static final String[] EXTENSIONS = {"jpg", "jpeg", "png", "gif"};
	/**
	 * The directory that will be scanned (excluding subdirectories)
	 */
	private final File baseDir;

	/**
	 * The helper used to create notifications of the tasks' progress
	 */
	private final NotificationHelper helper;

	public ImageDeletionTask(Context context, File baseDir, String contentTitle, int notifId) {
		this.baseDir = baseDir;
		this.helper = NotificationHelper.getInstance(context, notifId);
		helper.builder().setContentTitle(contentTitle)
				.setTicker(contentTitle)
				.setOngoing(true)
				.setAutoCancel(true);
	}

	@Override
	protected void onPreExecute() {
		updateBuilderProgress(0, 0);
		helper.notifyManager();
	}

	@Override
	protected Number[] doInBackground(Void... params) {
		// Get a list of files
		List<File> potentialFiles = new ArrayList<>(FileUtils.listFiles(baseDir, EXTENSIONS, false));

		List<File> comicFiles = new ArrayList<>();
		long totalBytes = 0;

		// Filter out files that don't fit the format of files downloaded by this app
		// (the comic id + the original extension, ex: "4321.jpg")
		for (File f : potentialFiles) {
			// Test if they fit the format
			String name = f.getName();
			if (name.substring(0, name.lastIndexOf(".")).matches("\\d+(\\d+)?")) {
				// Numeric string
				comicFiles.add(f);
			}
		}

		int deletionCount = 0;
		int totalFilesForDeletion = comicFiles.size();

		for (File f : comicFiles) {
			// Get the length before it's deleted
			long size = f.length();
			if (!f.delete()) {
				Log.w(Constants.TAG, "Unable to delete " + f.getAbsolutePath());
			} else {
				// Update the notification progress
				deletionCount++;
				totalBytes += size;
				onProgressUpdate(deletionCount, totalFilesForDeletion, totalBytes);
				Log.d(Constants.TAG, "Deleted " + f.getAbsolutePath());
			}
		}

		// Done
		return new Number[] {deletionCount, totalBytes};
	}

	@Override
	protected void onProgressUpdate(Number... values) {
		int deletionCount = (int) values[0];
		int totalFilesForDeletion = (int) values[1];
		long totalBytes = (long) values[2];

		// Update the progress bar and textual progress
		helper.builder().setProgress(totalFilesForDeletion, deletionCount, false);
		updateBuilderProgress(deletionCount, totalBytes);
		helper.notifyManager();
	}

	@Override
	protected void onPostExecute(Number[] data) {
		helper.builder().setOngoing(false);
		// Hide the progress bar
		helper.builder().setProgress(0, 0, false);
		helper.notifyManager();
	}

	/**
	 * Updates the Notification Builder's content text by formatting {@link #PROGRESS_TEMPLATE}
	 * @param deletionCount How many files have been deleted so far
	 * @param totalBytes How many bytes have been deleted so far
	 */
	private void updateBuilderProgress(int deletionCount, long totalBytes) {
		// One file: "Deleted 1 file (62 KB)"
		// Multiple or no files: "Deleted 2 files (87 KB)"
		helper.builder().setContentText(String.format(PROGRESS_TEMPLATE, deletionCount, deletionCount != 1 ? "s" : "",
				FileUtils.byteCountToDisplaySize(totalBytes)));
	}
}
