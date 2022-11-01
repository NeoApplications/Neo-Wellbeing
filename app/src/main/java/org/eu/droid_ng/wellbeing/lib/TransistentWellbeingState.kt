package org.eu.droid_ng.wellbeing.lib

import android.app.Activity
import android.content.Context
import org.eu.droid_ng.wellbeing.prefs.FocusModeActivity
import java.util.function.Consumer

class TransistentWellbeingState private constructor(private var context: Context, private var client: WellbeingStateClient) {
	companion object {
		@JvmStatic
		fun use(context: Context, callback: Consumer<TransistentWellbeingState>) {
			val tw = TransistentWellbeingState(context, WellbeingStateClient(context))
			tw.later { callback.accept(tw) }
		}
	}

	private var service: WellbeingService? = null

	fun later(callback: Runnable) {
		if (service?.isDestroyed() != false) {
			service = null
			if (client.isServiceRunning()) {
				client.doBindService {
					service = it
					callback.run()
				}
				return
			}
		}
		callback.run()
	}

	fun getService(): WellbeingService? {
		if (service?.isDestroyed() != false) {
			return null
		}
		return service
	}

	fun getState(): State {
		return getService()?.getState() ?: State(State.STATE_UPDATE_FAILURE)
	}

	fun getAppState(packageName: String): State {
		return getService()?.getAppState(packageName) ?: State(State.STATE_UPDATE_FAILURE)
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

	fun takeFocusModeBreakWithDialog(activityContext: Activity, endActivity: Boolean, packageNames: Array<String>) {
		TODO()
	}

	fun takeFocusModeBreak(breakMins: Int) {
		TODO()
	}

	fun getBoolSetting(setting: String): Boolean {
		TODO()
	}
}