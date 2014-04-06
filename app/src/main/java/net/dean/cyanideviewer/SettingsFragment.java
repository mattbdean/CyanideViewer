package net.dean.cyanideviewer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

public class SettingsFragment extends PreferenceFragment {
	public SettingsFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences
		addPreferencesFromResource(R.xml.preferences);

		Preference p = findPreference("pref_wipe_db");
		if (p != null) {
			p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					new SimpleDialog("Wipe database?", "This will erase the entire database, which includes records of every comic you've seen.", "Wipe", new OnConfirmedListener() {
						@Override
						public void onConfirmed() {
							// Truncate the database
							Log.w(CyanideViewer.TAG, "Truncating the database after user request");
							CyanideViewer.getComicDao().deleteAllComics();
						}
					}).show(getFragmentManager(), "wipe_db");
					return true;
				}
			});
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
