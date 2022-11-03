package org.eu.droid_ng.wellbeing.lib

import android.app.Activity
import android.content.Context
import org.eu.droid_ng.wellbeing.Wellbeing
import org.eu.droid_ng.wellbeing.lib.BugUtils.Companion.BUG
import java.util.function.Consumer

class WellbeingService(private val context: Context) {
	companion object {
		@JvmStatic
		fun get(): WellbeingService {
			return Wellbeing.getService()
		}
	}
	private var host: WellbeingStateHost? = null

	fun bindToHost(newhost: WellbeingStateHost?) {
		host = newhost
		if (host != null) {
			onServiceStartedCallbacks.toArray(arrayOf<Runnable>()).forEach {
				it.run()
				onServiceStartedCallbacks.remove(it)
			}
		}
	}
	private val stateCallbacks: ArrayList<Consumer<WellbeingService>> = ArrayList()

	fun addStateCallback(callback: Consumer<WellbeingService>) {
		stateCallbacks.add(callback)
	}

	fun removeStateCallback(callback: Consumer<WellbeingService>) {
		stateCallbacks.remove(callback)
	}

	private fun onStateChanged() {
		stateCallbacks.forEach { it.accept(this) }
	}

	private val onServiceStartedCallbacks: ArrayList<Runnable> = ArrayList()

	fun startService(lateNotify: Boolean = false) {
		if (host != null) {
			return
		}
		val client = WellbeingStateClient(context)
		client.startService(lateNotify)
	}

	fun startServiceAnd(lateNotify: Boolean = false, callback: Runnable? = null) {
		if (host != null) {
			callback?.run()
			return
		}
		if (callback != null) {
			onServiceStartedCallbacks.add(callback)
		}
		startService(lateNotify)
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

	fun getAppState(packageName: String): State {
		TODO("Not yet implemented")
	}


	fun onAppTimerExpired(observerId: Int, uniqueObserverId: String) {
		TODO()
	}

	fun onBootCompleted() {

	}

	fun onManuallyUnsuspended(packageName: String) {
		TODO()
	}

	fun onNotificationActionClick(action: String) {
		TODO()
	}

	fun enableFocusMode() {
		TODO()
	}

	fun disableFocusMode() {
		TODO()
	}

	fun endFocusModeBreak() {
		TODO()
	}

	fun takeFocusModeBreakWithDialog(activityContext: Activity, endActivity: Boolean, packageNames: Array<String>?) {
		TODO()
	}

	fun takeFocusModeBreak(breakMins: Int) {
		TODO()
	}

	fun getBoolSetting(setting: String): Boolean {
		TODO()
	}
}