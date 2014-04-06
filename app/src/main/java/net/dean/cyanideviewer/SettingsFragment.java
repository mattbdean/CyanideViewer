package net.dean.cyanideviewer;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by matthew on 3/29/14.
 */
public class SettingsFragment extends PreferenceFragment {
	public SettingsFragment() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Load the preferences
		addPreferencesFromResource(R.xml.preferences);

		final MultiSelectListPreference wipeData = (MultiSelectListPreference) findPreference("pref_reset_data");
		if (wipeData != null) {
			wipeData.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(CyanideViewer.getContext());
					Log.d(CyanideViewer.TAG, prefs.getStringSet("pref_reset_data", null).toString());
					return true;
				}
			});
		}
	}
}
