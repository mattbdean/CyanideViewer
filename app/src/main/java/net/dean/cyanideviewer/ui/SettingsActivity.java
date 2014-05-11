package net.dean.cyanideviewer.ui;

import android.app.Activity;
import android.os.Bundle;

import net.dean.cyanideviewer.R;

/**
 * This activity is responsible for showing a SettingsFragment
 */
public class SettingsActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_settings);

		// Display the fragment as the main content
		getFragmentManager().beginTransaction()
			.replace(R.id.settings_fragment_placeholder, new SettingsFragment())
			.commit();
    }
}
