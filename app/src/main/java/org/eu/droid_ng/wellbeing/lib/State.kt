package org.eu.droid_ng.wellbeing.lib

class State(private val value: Int) {
	companion object {
		/* Update of State (partially) failed */
		const val STATE_UPDATE_FAILURE = 1
		/* Focus mode currently: Enabled (also set if break is taken at the moment) */
		const val STATE_FOCUS_MODE_ENABLED = 2
		/* Focus mode currently: Break (Global) */
		const val STATE_FOCUS_MODE_GLOBAL_BREAK = 4
		/* Focus mode currently: Break (Per-App) */
		const val STATE_FOCUS_MODE_APP_BREAK = 8
		/* Manual suspension */
		const val STATE_MANUAL_SUSPEND = 16
	}

	private var valid: Boolean = true

	private fun assertValid() {
		if (!valid)
			throw IllegalStateException("tried to read from invalidated state")
	}

	private fun isPresent(bitmask: Int): Boolean {
		assertValid()
		return (value and bitmask) > 0
	}

	fun isValid(): Boolean {
		return valid
	}

	fun invalidate() {
		assertValid()
		valid = false
	}

	fun toInt(): Int {
		assertValid()
		return value
	}

	fun hasUpdateFailed(): Boolean {
		return isPresent(STATE_UPDATE_FAILURE)
	}

	fun isFocusModeEnabled(): Boolean {
		return isPresent(STATE_FOCUS_MODE_ENABLED)
	}

	fun isOnFocusModeBreakGlobal(): Boolean {
		return isPresent(STATE_FOCUS_MODE_GLOBAL_BREAK)
	}

	fun isOnFocusModeBreakPartial(): Boolean {
		return isPresent(STATE_FOCUS_MODE_APP_BREAK)
	}

	fun isSuspendedManually(): Boolean {
		return isPresent(STATE_MANUAL_SUSPEND)
	}
}