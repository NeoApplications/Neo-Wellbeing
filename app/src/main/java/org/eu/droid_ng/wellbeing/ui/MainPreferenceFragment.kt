package org.eu.droid_ng.wellbeing.ui

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.prefs.AppTimers
import org.eu.droid_ng.wellbeing.prefs.BedtimeMode
import org.eu.droid_ng.wellbeing.prefs.FocusModeActivity
import org.eu.droid_ng.wellbeing.prefs.ManualSuspendActivity
import org.eu.droid_ng.wellbeing.prefs.SettingsActivity

class MainPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)
        findPreference<Preference>("dashboard")?.apply {
            onPreferenceClickListener =
                Preference.OnPreferenceClickListener { _: Preference? ->
                    startActivity(
                        Intent(
                            activity,
                            DashboardActivity::class.java
                        )
                    )
                    true
                }
        }
        findPreference<Preference>("focus_mode")?.apply {
            onPreferenceClickListener =
                Preference.OnPreferenceClickListener { _: Preference? ->
                    startActivity(
                        Intent(
                            activity,
                            FocusModeActivity::class.java
                        )
                    )
                    true
                }
        }
        findPreference<Preference>("bedtime_mode")?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener { _: Preference? ->
                startActivity(Intent(activity, BedtimeMode::class.java))
                true
            }
        }
        findPreference<Preference>("manual")?.apply {
            val show = requireActivity().getSharedPreferences("service", 0)
                .getBoolean("manual", false)
            isVisible = show
            onPreferenceClickListener = Preference.OnPreferenceClickListener { _: Preference? ->
                startActivity(
                    Intent(
                        activity,
                        ManualSuspendActivity::class.java
                    )
                )
                true
            }
        }
        findPreference<Preference>("timers")?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener { _: Preference? ->
                startActivity(Intent(activity, AppTimers::class.java))
                true
            }
        }
        findPreference<Preference>("settings")?.apply {
            onPreferenceClickListener = Preference.OnPreferenceClickListener { _: Preference? ->
                startActivity(
                    Intent(
                        activity,
                        SettingsActivity::class.java
                    )
                )
                true
            }
        }
    }
}
