package net.dean.cyanideviewer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import net.dean.cyanideviewer.api.CyanideApi;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This fragment is responsible for showing the user Preferences defined in res/xml/preferences.xml
 */
public class SettingsFragment extends PreferenceFragment {
	/** The NotificationManager that will be used to send notifications to the OS */
	private NotificationManager notificationManager;

	/** The ID of the notification created by this app.*/
	private static final int DELETE_COMIC_NOTIF_ID = 0;

	/** The extensions to search and destroy */
	private static final String[] EXTENSIONS = {"jpg", "jpeg", "png", "gif"};

	/**
	 * Instantiates a new SettingsFragment
	 */
	public SettingsFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.notificationManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);

		// Load the preferences
		addPreferencesFromResource(R.xml.preferences);

		Preference wipeDb = findPreference("pref_wipe_db");
		if (wipeDb != null) {
			wipeDb.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					new SimpleDialog("Wipe database?",
							"This will erase the entire database, which includes records of every comic you've seen.",
							"Wipe",
							new OnConfirmedListener() {
								@Override
								public void onConfirmed() {
									onWipeDatabase();
								}
							}
					).show(getFragmentManager(), "wipe_db");
					return true;
				}
			});
		}

		Preference comics = findPreference("pref_delete_comics");
		if (comics != null) {
			comics.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					new SimpleDialog("Delete downloaded images?",
							"This will delete all the images that you've downloaded",
							"Delete",
							new OnConfirmedListener() {
								@Override
								public void onConfirmed() {
									onDeleteImages();
								}
							}).show(getFragmentManager(), "delete_images");

					return true;
				}
			});
		}
	}

	/**
	 * Called when the user has confirmed that they want to wipe the database
	 */
	private void onWipeDatabase() {
		// Truncate the database
		Log.w(CyanideViewer.TAG, "Truncating the database after user request");
		CyanideViewer.getComicDao().deleteAll();
		Toast.makeText(getActivity().getApplicationContext(), "Database wiped.", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Called when the user has confirmed that they want to delete all comics
	 */
	private void onDeleteImages() {
		// Start removing files
		new ImageDeletionTask(CyanideApi.instance().getSavedImageDirectory(),
				"Deleting saved comics", DELETE_COMIC_NOTIF_ID).execute();
	}

	/**
	 * This class is responsible for deleting image files (*.png, *.jpeg, *.jpg, *.gif) from a certain
	 * directory
	 */
	private class ImageDeletionTask extends AsyncTask<Void, Number, Number[]> {
		/**
		 * The template used to format the content text of the notification. It contains three
		 * %s tags. The first represents the amount of files that have been deleted. The second
		 * represents whether or not "files" should be plural. The third represents how many bytes
		 * (in a human readable format) have been deleted.
		 */
		private static final String PROGRESS_TEMPLATE = "Deleted %s file%s (%s)";

		/**
		 * The directory that will be scanned (excluding subdirectories)
		 */
		private File baseDir;
		private NotificationHelper helper;

		public ImageDeletionTask(File baseDir, String contentTitle, int notifId) {
			this.baseDir = baseDir;
			this.helper = NotificationHelper.getInstance(getActivity().getApplicationContext(), notifId);
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
					Log.w(CyanideViewer.TAG, "Unable to delete " + f.getAbsolutePath());
				} else {
					// Update the notification progress
					deletionCount++;
					totalBytes += size;
					onProgressUpdate(deletionCount, totalFilesForDeletion, totalBytes);
					Log.d(CyanideViewer.TAG, "Deleted " + f.getAbsolutePath());
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

	/**
	 * Simple listener class to be used with SimpleDialog
	 */
	private interface OnConfirmedListener {
		/**
		 * Called when the user confirms an action
		 */
		public void onConfirmed();
	}

	/**
	 * This class allows for simple yes/no decisions through the use of a dialog
	 */
	private class SimpleDialog extends DialogFragment {
		/**
		 * The title of the dialog
		 */
		private String title;

		/**
		 * The body (message) of the dialog
		 */
		private String body;

		/**
		 * The text of the "positive" button (ex: "Yes", "Continue", etc.)
		 */
		private String positiveButtonText;

		/**
		 * The listener that is called if the user confirms the action
		 */
		private OnConfirmedListener listener;

		private SimpleDialog(String title, String body, String positiveButtonText, OnConfirmedListener listener) {
			this.title = title;
			this.body = body;
			this.positiveButtonText = positiveButtonText;
			this.listener = listener;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(title)
					.setMessage(body)
					.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Confirmed
							listener.onConfirmed();
							dismiss();
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Canceled
							dismiss();
						}
					});
			return builder.create();
		}
	}
}
