package net.dean.cyanideviewer.ui.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import net.dean.cyanideviewer.Callback;
import net.dean.cyanideviewer.Constants;
import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.R;
import net.dean.cyanideviewer.api.CyanideApi;

/**
 * This fragment is responsible for showing the user Preferences defined in res/xml/preferences.xml
 */
public class SettingsFragment extends PreferenceFragment {

	/**
	 * Instantiates a new SettingsFragment
	 */
	public SettingsFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences
		addPreferencesFromResource(R.xml.preferences);

		Preference wipeDb = findPreference("pref_wipe_db");
		if (wipeDb != null) {
			wipeDb.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					SimpleDialog.newInstance("Wipe database?",
							"This will erase the entire database, which includes records of every comic you've seen.",
							"Wipe",
							new Callback<Void>() {
								@Override
								public void onComplete(Void result) {
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
					SimpleDialog.newInstance("Delete downloaded images?",
							"This will delete all the images that you've downloaded",
							"Delete",
							new Callback<Void>() {
								@Override
								public void onComplete(Void v) {
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
		Log.w(Constants.TAG, "Truncating the database after user request");
		CyanideViewer.getComicDao().deleteAll();
		Toast.makeText(getActivity().getApplicationContext(), "Database wiped.", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Called when the user has confirmed that they want to delete all comics
	 */
	private void onDeleteImages() {
		// Start removing files
		new ImageDeletionTask(getActivity(), CyanideApi.instance().getSavedImageDirectory(),
				"Deleting saved comics", Constants.NOTIF_DELETE_COMIC).execute();
	}

}
