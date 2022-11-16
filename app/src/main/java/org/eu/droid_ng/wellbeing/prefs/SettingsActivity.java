package org.eu.droid_ng.wellbeing.prefs;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreference;
import androidx.preference.SwitchPreferenceCompat;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.BugUtils;
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate;

import java.util.Map;
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
			applyMaterial3(getPreferenceScreen());

			if (!PackageManagerDelegate.canSetNeutralButtonAction()) {
				((Preference) Objects.requireNonNull(findPreference("manual_dialog"))).setEnabled(false);
				((Preference) Objects.requireNonNull(findPreference("focus_dialog"))).setEnabled(false);
			}
			if (Objects.requireNonNull(BugUtils.get()).hasBugs()) {
				Map<String, String> bugMap = Objects.requireNonNull(BugUtils.get()).getBugs();
				String[] a = bugMap.keySet().toArray(new String[0]);
				Preference bp = Objects.requireNonNull(findPreference("bugs"));
				bp.setVisible(true);
				bp.setOnPreferenceClickListener(p -> {
					new AlertDialog.Builder(requireActivity())
							.setTitle(R.string.bug_viewer)
							.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, a), (dialog, pos) -> {
								String key = a[pos];
								String value = bugMap.get(a[pos]);
								new AlertDialog.Builder(requireActivity())
										.setTitle(key)
										.setMessage(value)
										.setPositiveButton(R.string.share, (d, which) -> {
											Intent sendIntent = new Intent();
											sendIntent.setAction(Intent.ACTION_SEND);
											sendIntent.putExtra(Intent.EXTRA_TEXT, value);
											sendIntent.setType("text/plain");

											Intent shareIntent = Intent.createChooser(sendIntent, null);
											startActivity(shareIntent);
										})
										.setNeutralButton(R.string.copy_to_clipboard, (d, which) -> {
											ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
											ClipData clip = ClipData.newPlainText("Bug report", value);
											clipboard.setPrimaryClip(clip);
											Toast.makeText(getActivity(), R.string.copied, Toast.LENGTH_LONG).show();
										})
										.setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
										.show();
							})
							.setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
							.show();
					return true;
				});
			}
		}

		public static void applyMaterial3(Preference p) {
			if (p instanceof PreferenceGroup) {
				PreferenceGroup pg = (PreferenceGroup) p;
				for (int i = 0; i < pg.getPreferenceCount(); i++) {
					applyMaterial3(pg.getPreference(i));
				}
			}
			if (p instanceof SwitchPreferenceCompat) {
				p.setWidgetLayoutResource(R.layout.preference_material_switch);
			}
		}
	}
}