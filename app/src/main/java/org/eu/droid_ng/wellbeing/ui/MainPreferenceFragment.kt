package org.eu.droid_ng.wellbeing.ui

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.eu.droid_ng.wellbeing.R

class MainPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)
        findPreference<Preference>("manual")?.apply {
            val show = requireActivity().getSharedPreferences("service", 0)
                .getBoolean("manual", false)
            isVisible = show
        }
    }
}
