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
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import net.dean.cyanideviewer.api.CyanideApi;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SettingsFragment extends PreferenceFragment {
	private NotificationManager notificationManager;
	private static final int DELETE_COMIC_NOTIF_ID = 0;

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

	private void onWipeDatabase() {
		// Truncate the database
		Log.w(CyanideViewer.TAG, "Truncating the database after user request");
		CyanideViewer.getComicDao().deleteAllComics();
		Toast.makeText(getActivity().getApplicationContext(), "Database wiped.", Toast.LENGTH_SHORT).show();
	}

	private void onDeleteImages() {
		final NotificationCompat.Builder builder = new NotificationCompat.Builder(this.getActivity());
		builder.setContentTitle("Deleting saved comics")
				.setSmallIcon(R.drawable.ic_action_delete)
				.setOngoing(true)
				.setAutoCancel(true);

		// Start removing files
		new AsyncTask<Void, Number, Number[]>() {

			@Override
			protected void onPreExecute() {
				notificationManager.notify(DELETE_COMIC_NOTIF_ID, builder.build());
			}

			@Override
			protected Number[] doInBackground(Void... params) {
				// Get a list of files
				List<File> potentialFiles = new ArrayList<>(FileUtils.listFiles(CyanideApi.instance().getSavedImageDirectory(),
						new String[]{"jpg", "jpeg", "png", "gif"}, false));

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
					if (!f.delete()) { // TODO !f.delete()
						Log.w(CyanideViewer.TAG, "Unable to delete " + f.getAbsolutePath());
					} else {
						// Update the notification progress
						deletionCount++;
						totalBytes += size;
						onProgressUpdate(deletionCount, totalFilesForDeletion, totalBytes);
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
				builder.setProgress(totalFilesForDeletion, deletionCount, false);
				builder.setContentText(String.format("Deleted %s comics (%s)", deletionCount,
								FileUtils.byteCountToDisplaySize(totalBytes)));
				notificationManager.notify(DELETE_COMIC_NOTIF_ID, builder.build());
			}

			@Override
			protected void onPostExecute(Number[] data) {
				builder.setOngoing(false);
				// Hide the progress bar
				builder.setProgress(0, 0, false);
				notificationManager.notify(DELETE_COMIC_NOTIF_ID, builder.build());
			}
		}.execute();
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
							listener.onConfirmed();
							dismiss();
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dismiss();
						}
					});
			return builder.create();
		}
	}
}
