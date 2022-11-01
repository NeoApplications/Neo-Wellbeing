package org.eu.droid_ng.wellbeing.lib

import android.content.Context
import org.eu.droid_ng.wellbeing.lib.BugUtils.Companion.BUG
import java.util.function.Consumer

class WellbeingService(private val context: Context, private val host: WellbeingStateHost) {
	private var destroyed = false
	fun onDestroy() {
		destroyed = true
	}
	fun isDestroyed(): Boolean {
		return destroyed
	}
	val stateCallbacks: List<Consumer<WellbeingService>> = ArrayList()
	private fun onStateChanged() {
		stateCallbacks.forEach { it.accept(this) }
	}

	/* **** main part **** */
	private var isFocusModeEnabled = false
	private var isFocusModeBreak /* global break */ = false

	// Service / Global state. Do not confuse with per-app state, that's using the same values.
	fun getState(): State {
		val value =
			(if (isFocusModeEnabled) State.STATE_FOCUS_MODE_ENABLED else 0) or
			(if (isFocusModeBreak) State.STATE_FOCUS_MODE_GLOBAL_BREAK else 0) or
			(if (false /*TODO*/) State.STATE_FOCUS_MODE_APP_BREAK else 0)
		return State(value)
	}

	fun setFocusMode(new: Boolean) {
		if (isFocusModeEnabled == new) {
			BUG("Focus mode value not changed in setFocusMode. current / new = $new")
			return
		}
		TODO()
	}

	fun getAppState(packageName: String): State {
		TODO("Not yet implemented")
	}
}