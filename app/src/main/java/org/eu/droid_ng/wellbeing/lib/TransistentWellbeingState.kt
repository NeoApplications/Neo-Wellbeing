package org.eu.droid_ng.wellbeing.lib

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate
import java.util.function.Consumer

class TransistentWellbeingState private constructor(val context: Context, private val state: GlobalWellbeingState?, val client: WellbeingStateClient) {

	companion object {
		const val STATE_SUSPEND_UNKNOWN_REASON = 1
		const val STATE_SUSPEND_FOCUS_MODE = 2
		const val STATE_SUSPEND_MANUAL = 4
		// Only set on INDIVIDUAL app breaks.
		const val STATE_UNSUSPEND_FOCUS_MODE_BREAK = 8
		const val STATE_UNKNOWN = 16
		const val STATE_APP_TIMER_SET = 32
		const val STATE_SUSPEND_APP_TIMER_EXPIRED = 64
		const val STATE_DESUSPEND_APP_TIMER_BREAK = 128
		const val STATE_UNSUSPEND_FOCUS_MODE_BREAK_GLOBAL = 256

		const val INTENT_ACTION_TAKE_BREAK = "org.eu.droid_ng.wellbeing.TAKE_BREAK"
		const val INTENT_ACTION_QUIT_BREAK = "org.eu.droid_ng.wellbeing.QUIT_BREAK"
		const val INTENT_ACTION_QUIT_FOCUS = "org.eu.droid_ng.wellbeing.QUIT_FOCUS"
		const val INTENT_ACTION_UNSUSPEND_ALL = "org.eu.droid_ng.wellbeing.UNSUSPEND_ALL"
		@JvmField val breakTimeOptions = intArrayOf(1, 3, 5, 10, 15)

		@JvmStatic fun get(context: Context, callback: Consumer<TransistentWellbeingState>) {
			val sc = WellbeingStateClient(context)
			if (sc.isServiceRunning()) {
				sc.doBindService { callback.accept(TransistentWellbeingState(context, it, sc)) }
			} else {
				callback.accept(TransistentWellbeingState(context, null, sc))
			}
		}

		@JvmStatic fun use(context: Context, callback: Consumer<TransistentWellbeingState>) {
			get(context) {
				callback.accept(it)
				it.doUnbindService()
			}
		}
	}

	private val ati: AppTimersInternal = AppTimersInternal.get(context)
	private val usm: UsageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
	private val pm: PackageManager = context.packageManager
	private val pmd = PackageManagerDelegate(pm)

	fun getAppState(packageName: String): Int {
		val hasAppTimer = ati.getTimeLimitForApp(packageName)
		val isAppOnBreak = if (hasAppTimer == null) false else ati.isAppOnBreak(packageName)
		val isAppTimerExpired = if (hasAppTimer == null) false else Utils.getTimeUsed(usm, arrayOf(packageName))!!.minus(hasAppTimer).toMinutes() <= 0
		val gwsLegacyState = state?.reasonMap?.getOrDefault(packageName, GlobalWellbeingState.REASON.REASON_UNKNOWN)?.toState() ?: 0
		val gwsHasBreak = state?.focusModeBreak ?: false
		return (if (hasAppTimer != null) STATE_APP_TIMER_SET else 0) or
				(if (isAppOnBreak) STATE_DESUSPEND_APP_TIMER_BREAK else 0) or
				(if (isAppTimerExpired) STATE_SUSPEND_APP_TIMER_EXPIRED else 0) or
				gwsLegacyState or
				(if (gwsHasBreak) STATE_UNSUSPEND_FOCUS_MODE_BREAK_GLOBAL else 0)
	}

	fun requireState(): GlobalWellbeingState {
		return state!!
	}

	fun doUnbindService() {
		if (state != null)
			client.doUnbindService()
	}

	fun onManuallyUnsuspended(packageName: String) {
		if (state != null) {
			state.onManuallyUnsuspended(packageName)
		} else {
			ati.appTimerSuspendHook(packageName)
		}
	}

	fun onAppTimerExpired(oid: Int, uoid: String) {
		ati.onBroadcastRecieve(oid, uoid)
	}

	fun onBootCompleted() {
		ati.onBootRecieved()
	}

	fun onNotificationActionClick(action: String?) {
		val client = WellbeingStateClient(context)
		if (INTENT_ACTION_TAKE_BREAK.equals(action)) {
			client.doBindService { state -> state!!.takeBreak(state.notificationBreakTime) }
		} else if (INTENT_ACTION_QUIT_BREAK.equals(action)) {
			client.doBindService { obj: GlobalWellbeingState? -> obj!!.endBreak() }
		} else if (INTENT_ACTION_QUIT_FOCUS.equals(action)) {
			client.doBindService({ it!!.disableFocusMode() }, false, true, true)
		} else if (INTENT_ACTION_UNSUSPEND_ALL.equals(action)) {
			client.doBindService { state -> state!!.manualUnsuspend(null, true) }
		}
	}
}