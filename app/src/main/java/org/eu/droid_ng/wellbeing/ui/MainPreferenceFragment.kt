package org.eu.droid_ng.wellbeing.ui

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.WellbeingService
import java.util.function.Consumer

class MainPreferenceFragment : PreferenceFragmentCompat() {

    private val service: WellbeingService by lazy {
        WellbeingService.get()
    }

    private val stateCallback = Consumer<WellbeingService> { _: WellbeingService ->
        // Update summary on new state
        updateSummary()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)
        findPreference<Preference>("manual")?.apply {
            val show = requireActivity().getSharedPreferences("service", 0)
                .getBoolean("manual", false)
            isVisible = show
        }
        // Update the summary of the preferences based on the state
        updateSummary()
        // Add state callback
        service.addStateCallback(stateCallback)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove state callback
        service.removeStateCallback(stateCallback)
    }

    private fun updateSummary() {
        val state = service.getState(false)
        findPreference<Preference>("bedtime_mode")?.apply {
            val isBedTimeModeEnabled = state.isBedtimeModeEnabled()
            summary = if (isBedTimeModeEnabled) getString(R.string.on) else getString(R.string.off)
        }
        findPreference<Preference>("timers")?.apply {
            val isAppTimerSet = state.isAppTimerSet()
            summary = if (isAppTimerSet) getString(R.string.on) else getString(R.string.off)
        }
        findPreference<Preference>("manual")?.apply {
            val isManuallySuspended = state.isSuspendedManually()
            summary = if (isManuallySuspended) getString(R.string.on) else getString(R.string.off)
        }
    }

}
