package net.dean.cyanideviewer.ui.settings;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.widget.Toast;

import com.nononsenseapps.filepicker.FilePickerActivity;

import net.dean.cyanideviewer.Callback;
import net.dean.cyanideviewer.Constants;
import net.dean.cyanideviewer.CyanideViewer;
import net.dean.cyanideviewer.R;
import net.dean.cyanideviewer.api.CyanideApi;

import java.io.File;

/**
 * This fragment is responsible for showing the user Preferences defined in res/xml/preferences.xml
 */
public class SettingsFragment extends PreferenceFragment {
	private static final int FILE_CODE = 0;

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
									wipeDatabase();
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
									deleteImages();
								}
							}
					).show(getFragmentManager(), "delete_images");

					return true;
				}
			});
		}

		Preference downloadLocation = findPreference(Constants.KEY_DOWNLOAD_LOCATION);
		// Set the message to the current value of the key

		SharedPreferences prefs = CyanideViewer.getPrefs();

		String currentDir;
		if (!prefs.contains(Constants.KEY_DOWNLOAD_LOCATION)) {
			currentDir = new File(Environment.getExternalStorageDirectory(), "CyanideViewer").getAbsolutePath();
			prefs.edit().putString(Constants.KEY_DOWNLOAD_LOCATION, currentDir).commit();
		} else {
			currentDir = prefs.getString(Constants.KEY_DOWNLOAD_LOCATION, null);
		}

		downloadLocation.setSummary(currentDir);

		downloadLocation.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				String prefDir = getPreferenceManager().getSharedPreferences().getString(Constants.KEY_DOWNLOAD_LOCATION,
						CyanideApi.instance().getSavedImageDirectory().getAbsolutePath());

				Intent i = new Intent(getActivity(), FilePickerActivity.class);
				i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
				i.putExtra(FilePickerActivity.EXTRA_START_PATH, prefDir);
				i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR);
				i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);

				startActivityForResult(i, FILE_CODE);
				return true;
			}
		});

	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();

			// Save it
			String location = new File(uri.getPath()).getAbsolutePath();
			getPreferenceManager().getSharedPreferences().edit().putString(Constants.KEY_DOWNLOAD_LOCATION,
					location);

			findPreference(Constants.KEY_DOWNLOAD_LOCATION).setSummary(location);
		}
	}

	/**
	 * Called when the user has confirmed that they want to wipe the database
	 */
	private void wipeDatabase() {
		// Truncate the database
		Log.w(Constants.TAG, "Truncating the database after user request");
		CyanideViewer.getComicDao().deleteAll();
		Toast.makeText(getActivity().getApplicationContext(), "Database wiped.", Toast.LENGTH_SHORT).show();
	}

	/**
	 * Called when the user has confirmed that they want to delete all comics
	 */
	private void deleteImages() {
		// Start removing files
		new ImageDeletionTask(getActivity(), CyanideApi.instance().getSavedImageDirectory(),
				"Deleting saved comics", Constants.NOTIF_DELETE_COMIC).execute();
	}

}
