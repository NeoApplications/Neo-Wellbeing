package org.eu.droid_ng.wellbeing.prefs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.WellbeingService
import org.eu.droid_ng.wellbeing.shared.BugUtils.Companion.formatDateForRender
import org.eu.droid_ng.wellbeing.shared.BugUtils.Companion.get
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate
import java.util.Objects

class SettingsActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.settings_activity)
		if (savedInstanceState == null) {
			supportFragmentManager
				.beginTransaction()
				.replace(R.id.settings, SettingsFragment())
				.commit()
		}
		setSupportActionBar(findViewById(R.id.topbar))
		val actionBar = checkNotNull(supportActionBar)
		actionBar.setDisplayHomeAsUpEnabled(true)
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}

	class SettingsFragment : PreferenceFragmentCompat() {
		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			preferenceManager.sharedPreferencesName = "service"
			setPreferencesFromResource(R.xml.root_preferences, rootKey)

			if (!PackageManagerDelegate.canSetNeutralButtonAction()) {
				(Objects.requireNonNull<Any?>(findPreference("manual_dialog")) as Preference).isEnabled =
					false
				(Objects.requireNonNull<Any?>(findPreference("focus_dialog")) as Preference).isEnabled =
					false
			}
			val bugs = get()!!.getBugs() + WellbeingService.get().getBugs()
			if (bugs.isNotEmpty()) {
				val bugMap = bugs.toList().sortedBy { it.first }.map { Pair(formatDateForRender(it.first), it.second) }
				val a = bugMap.map { it.first }.toTypedArray<String>()
				val bp = findPreference<Preference>("bugs")!!
				bp.isVisible = true
				bp.onPreferenceClickListener =
					Preference.OnPreferenceClickListener {
						AlertDialog.Builder(requireActivity())
							.setTitle(R.string.bug_viewer)
							.setAdapter(
								ArrayAdapter(
									requireActivity(),
									android.R.layout.simple_list_item_1,
									a
								)
							) { _, pos ->
								val key = a[pos]
								val value = bugMap[pos].second
								AlertDialog.Builder(requireActivity())
									.setTitle(key)
									.setMessage(value)
									.setPositiveButton(R.string.share) { _, _ ->
										val sendIntent = Intent()
										sendIntent.setAction(Intent.ACTION_SEND)
										sendIntent.putExtra(Intent.EXTRA_TEXT, value)
										sendIntent.setType("text/plain")

										val shareIntent = Intent.createChooser(sendIntent, null)
										startActivity(shareIntent)
									}
									.setNeutralButton(R.string.copy_to_clipboard) { _, _ ->
										val clipboard = requireActivity().getSystemService(
											CLIPBOARD_SERVICE
										) as ClipboardManager
										val clip = ClipData.newPlainText("Bug report", value)
										clipboard.setPrimaryClip(clip)
										if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
											// T+ have built in indicator
											Toast.makeText(
												activity,
												R.string.copied,
												Toast.LENGTH_LONG
											).show()
										}
									}
									.setNegativeButton(R.string.cancel) { _, _ -> }
									.show()
							}
							.setNegativeButton(R.string.cancel) { _, _ -> }
							.show()
						true
					}
			}
		}
	}
}