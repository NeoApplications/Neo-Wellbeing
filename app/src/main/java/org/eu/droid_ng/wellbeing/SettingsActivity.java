package org.eu.droid_ng.wellbeing;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings_activity);
		if (savedInstanceState == null) {
			getSupportFragmentManager()
					.beginTransaction()
					.replace(R.id.settings, new SettingsFragment())
					.commit();
		}
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}

	public static class SettingsFragment extends PreferenceFragmentCompat {
		@Override
		public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
			getPreferenceManager().setSharedPreferencesName("service");
			setPreferencesFromResource(R.xml.root_preferences, rootKey);
			if (!PackageManagerDelegate.canSetNeutralButtonAction()) {
				((Preference) Objects.requireNonNull(findPreference("manual_dialog"))).setEnabled(false);
				((Preference) Objects.requireNonNull(findPreference("focus_dialog"))).setEnabled(false);
			}
		}
	}
}