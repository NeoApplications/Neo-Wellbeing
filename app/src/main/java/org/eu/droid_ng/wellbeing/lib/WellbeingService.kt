package org.eu.droid_ng.wellbeing.lib

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AlertDialog
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.Wellbeing
import org.eu.droid_ng.wellbeing.broadcast.NotificationBroadcastReciever
import org.eu.droid_ng.wellbeing.lib.BugUtils.Companion.BUG
import org.eu.droid_ng.wellbeing.prefs.MainActivity
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate.SuspendDialogInfo
import org.eu.droid_ng.wellbeing.ui.TakeBreakDialogActivity
import java.util.*
import java.util.function.Consumer


class WellbeingService(private val context: Context) {
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
		updateServiceStatus()
		stateCallbacks.forEach { it.accept(this) }
	}

	private val onServiceStartedCallbacks: ArrayList<Runnable> = ArrayList()

	private fun startService(lateNotify: Boolean = false) {
		if (host != null) {
			return
		}
		val client = WellbeingStateClient(context)
		client.startService(lateNotify)
	}

	private fun startServiceAnd(lateNotify: Boolean = false, callback: Runnable? = null) {
		loadSettings()
		if (host != null) {
			callback?.run()
			return
		}
		if (callback != null) {
			onServiceStartedCallbacks.add(callback)
		}
		startService(lateNotify)
	}

	private fun stopService() {
		host?.stop()
	}

	companion object {
		@JvmStatic
		fun get(): WellbeingService {
			return Wellbeing.getService()
		}

	/* **** main part of service starts here **** */

		const val INTENT_ACTION_TAKE_BREAK = "org.eu.droid_ng.wellbeing.TAKE_BREAK"
		const val INTENT_ACTION_QUIT_BREAK = "org.eu.droid_ng.wellbeing.QUIT_BREAK"
		const val INTENT_ACTION_QUIT_FOCUS = "org.eu.droid_ng.wellbeing.QUIT_FOCUS"
		const val INTENT_ACTION_UNSUSPEND_ALL = "org.eu.droid_ng.wellbeing.UNSUSPEND_ALL"
		@JvmField val breakTimeOptions = intArrayOf(1, 3, 5, 10, 15)
	}

	private val handler = Handler.createAsync(context.mainLooper)
	private val pm: PackageManager = context.packageManager
	private val pmd: PackageManagerDelegate = PackageManagerDelegate(pm)

	@JvmField var focusModeAllApps = true
	@JvmField var focusModeBreakTimeDialog = -1
	@JvmField var focusModeBreakTimeNotification = -1
	@JvmField var manualSuspendDialog = true
	@JvmField var manualSuspendAllApps = true

	private fun loadSettings() {
		val prefs = context.getSharedPreferences("service", 0)
		focusModeBreakTimeNotification = Integer.parseInt(prefs.getString("focus_notification", focusModeBreakTimeNotification.toString()) ?: focusModeBreakTimeNotification.toString())
		focusModeBreakTimeDialog = Integer.parseInt(prefs.getString("focus_dialog", focusModeBreakTimeDialog.toString()) ?: focusModeBreakTimeDialog.toString())
		manualSuspendDialog = prefs.getBoolean("manual_dialog", manualSuspendDialog)
		manualSuspendAllApps = prefs.getBoolean("manual_all", manualSuspendAllApps)
		focusModeAllApps = prefs.getBoolean("focus_all", focusModeAllApps)
	}

	init {
		loadSettings()
	}

	private var isFocusModeEnabled = false
	private var isFocusModeBreak /* global break */ = false
	private var perAppState: HashMap<String /* packageName */, Int /* does NOT contain global flags like FOCUS_MODE_ENABLED or FOCUS_MODE_GLOBAL_BREAK, so always use getAppState() when reading */> = HashMap()

	// Service / Global state. Do not confuse with per-app state, that's using the same values.
	@JvmOverloads
	fun getState(appShim: Boolean = true): State {
		val value =
			(if (isFocusModeEnabled) State.STATE_FOCUS_MODE_ENABLED else 0) or
			(if (isFocusModeBreak) State.STATE_FOCUS_MODE_GLOBAL_BREAK else 0) or
			(if (appShim && (perAppState.entries.stream().filter { (it.value and State.STATE_FOCUS_MODE_APP_BREAK) > 0 }.findAny().isPresent)) State.STATE_FOCUS_MODE_APP_BREAK else 0) or
			(if (appShim && (perAppState.entries.stream().filter { (it.value and State.STATE_MANUAL_SUSPEND) > 0 }.findAny().isPresent)) State.STATE_MANUAL_SUSPEND else 0)
		return State(value)
	}

	fun getAppState(packageName: String): State {
		var value = perAppState.getOrDefault(packageName, 0)

		/* apply matching global flags */
		val global = getState(false).toInt()
		if ((value and State.STATE_FOCUS_MODE_ENABLED) > 0) {
			value = value or (global and State.STATE_FOCUS_MODE_GLOBAL_BREAK)
		}

		return State(value)
	}

	fun onAppTimerExpired(observerId: Int, uniqueObserverId: String) {
		TODO("$observerId$uniqueObserverId")
	}

	fun onBootCompleted() {

	}

	fun updateServiceStatus() {
		val state = getState()
		val needServiceRunning = state.isFocusModeEnabled() || state.isSuspendedManually()
		val next = {
			if (state.isFocusModeEnabled()) {
				if (state.isOnFocusModeBreakGlobal()) {
					host?.updateNotification(
						R.string.focus_mode,
						R.string.notification_focus_mode_break,
						R.drawable.ic_stat_name,
						arrayOf(
							host?.buildAction(
								R.string.focus_mode_break_end, R.drawable.ic_take_break, Intent(
									context,
									NotificationBroadcastReciever::class.java
								).setAction(INTENT_ACTION_QUIT_BREAK), true
							),
							host?.buildAction(
								R.string.focus_mode_off, R.drawable.ic_stat_name, Intent(
									context,
									NotificationBroadcastReciever::class.java
								).setAction(INTENT_ACTION_QUIT_FOCUS), true
							)
						),
						Intent(context, MainActivity::class.java)
					)
				} else {
					host?.updateNotification(
						R.string.focus_mode,
						R.string.notification_focus_mode,
						R.drawable.ic_stat_name,
						arrayOf(
							if (focusModeBreakTimeNotification == -1) host?.buildAction(
								R.string.focus_mode_break, R.drawable.ic_take_break, Intent(
									context,
									TakeBreakDialogActivity::class.java
								), false
							) else host?.buildAction(
								R.string.focus_mode_break, R.drawable.ic_take_break, Intent(
									context,
									NotificationBroadcastReciever::class.java
								).setAction(INTENT_ACTION_TAKE_BREAK), true
							),
							host?.buildAction(
								R.string.focus_mode_off, R.drawable.ic_stat_name, Intent(
									context,
									NotificationBroadcastReciever::class.java
								).setAction(INTENT_ACTION_QUIT_FOCUS), true
							)
						),
						Intent(context, MainActivity::class.java)
					)
				}
			} else if (state.isSuspendedManually()) {
				host?.updateNotification(
					R.string.notification_title,
					R.string.notification_manual,
					R.drawable.ic_stat_name,
					arrayOf(
						host?.buildAction(
							R.string.unsuspend_all, R.drawable.ic_stat_name, Intent(
								context,
								NotificationBroadcastReciever::class.java
							).setAction(INTENT_ACTION_UNSUSPEND_ALL), true
						)
					),
					Intent(context, MainActivity::class.java)
				)
			} else {
				host?.updateDefaultNotification()
			}
		}
		if (needServiceRunning) {
			if (host == null) {
				startServiceAnd {
					next()
				}
			} else {
				next()
			}
		} else {
			if (host != null) {
				stopService()
			}
			next()
		}
	}

	fun onManuallyUnsuspended(packageName: String) {
		val state = getAppState(packageName)
		if (state.isFocusModeEnabled() && !(state.isOnFocusModeBreakGlobal() || state.isOnFocusModeBreakPartial())) {
			if (focusModeAllApps) {
				takeFocusModeBreak(focusModeBreakTimeDialog)
			} else {
				takeFocusModeBreak(arrayOf(packageName), focusModeBreakTimeDialog)
			}
		} else if (state.isSuspendedManually()) {
			if (manualSuspendAllApps) {
				manualUnsuspend(null) // unsuspend all
			} else {
				manualUnsuspend(arrayOf(packageName))
			}
		} else {
			BUG("Unable to handle manual unsuspend")
		}
	}

	fun onNotificationActionClick(action: String) {
		when (action) {
			INTENT_ACTION_UNSUSPEND_ALL -> {
				manualUnsuspend(null)
			}
			INTENT_ACTION_TAKE_BREAK -> {
				takeFocusModeBreak(focusModeBreakTimeNotification)
			}
			INTENT_ACTION_QUIT_BREAK -> {
				endFocusModeBreak()
			}
			INTENT_ACTION_QUIT_FOCUS -> {
				disableFocusMode()
			}
			else -> {
				BUG("invalid notification action")
			}
		}
	}

	private fun updateSuspendStatusForApp(packageName: String) {
		val state = getAppState(packageName)
		val f: Array<String> = if (state.isSuspendedManually()) {
			val di = SuspendDialogInfo.Builder()
				.setTitle(R.string.dialog_title)
				.setMessage(R.string.dialog_message)
				.setIcon(R.drawable.ic_baseline_app_blocking_24)
				.setNeutralButtonText(if (!manualSuspendDialog) R.string.dialog_btn_settings else (if (manualSuspendAllApps) R.string.unsuspend_all else R.string.unsuspend))
				.setNeutralButtonAction(if (!manualSuspendDialog) SuspendDialogInfo.BUTTON_ACTION_MORE_DETAILS else SuspendDialogInfo.BUTTON_ACTION_UNSUSPEND)
				.build()
			pmd.setPackagesSuspended(arrayOf(packageName), true, null, null, di)
		} else if (state.isFocusModeEnabled() && !(state.isOnFocusModeBreakGlobal() || state.isOnFocusModeBreakPartial())) {
			val label: CharSequence = try {
				pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0))
			} catch (e: PackageManager.NameNotFoundException) {
				BUG("tried to suspend nonexistant app: $packageName")
				return
			}
			val di = SuspendDialogInfo.Builder()
				.setTitle(R.string.focus_mode_enabled)
				.setMessage(context.getString(R.string.focus_mode_dialog, label))
				.setIcon(R.drawable.ic_focus_mode)
				.setNeutralButtonText(if (focusModeBreakTimeDialog == -1) R.string.dialog_btn_settings else context.resources.getIdentifier("break_dialog_$focusModeBreakTimeDialog", "string", context.packageName))
				.setNeutralButtonAction(if (focusModeBreakTimeDialog == -1) SuspendDialogInfo.BUTTON_ACTION_MORE_DETAILS else SuspendDialogInfo.BUTTON_ACTION_UNSUSPEND)
				.build()
			pmd.setPackagesSuspended(arrayOf(packageName), true, null, null, di)
		} else {
			pmd.setPackagesSuspended(arrayOf(packageName), false, null, null, null)
		}
		for (s in f) {
			BUG("Failed to (un)suspend package: $s")
		}
	}

	private fun setFocusModeStateForPkgInternal(s: String, suspend: Boolean, forBreak: Boolean, forAppBreak: Boolean) {
		if (suspend) {
			perAppState[s] = (perAppState.getOrDefault(s, 0) or State.STATE_FOCUS_MODE_ENABLED) and State.STATE_FOCUS_MODE_APP_BREAK.inv()
		} else {
			if (forBreak) {
				if (forAppBreak) {
					perAppState[s] = perAppState.getOrDefault(s, 0) or (State.STATE_FOCUS_MODE_APP_BREAK)
				}
			} else {
				perAppState[s] = perAppState.getOrDefault(s, 0) and (State.STATE_FOCUS_MODE_ENABLED.inv() and State.STATE_FOCUS_MODE_APP_BREAK.inv())
			}
		}

		updateSuspendStatusForApp(s)
	}

	fun enableFocusMode() {
		val spref = context.getSharedPreferences("appLists", 0)
		val st = spref.getStringSet("focus_mode", null)
		if (st == null) {
			BUG("st == null")
			return
		}

		isFocusModeEnabled = true
		isFocusModeBreak = false

		for (s in st)
			setFocusModeStateForPkgInternal(s, suspend = true, forBreak = false, forAppBreak = false)

		onStateChanged()
	}

	fun disableFocusMode() {
		val spref = context.getSharedPreferences("appLists", 0)
		val st = spref.getStringSet("focus_mode", null)
		if (st == null) {
			BUG("st == null")
			return
		}

		if (isFocusModeBreak) {
			handler.removeCallbacks(breakEndedCallback)
		}
		oneAppUnsuspendCallbacks.forEach { handler.removeCallbacks(it) }
		oneAppUnsuspendCallbacks.clear()

		isFocusModeEnabled = false
		isFocusModeBreak = false

		for (s in st)
			setFocusModeStateForPkgInternal(s, suspend = false, forBreak = false, forAppBreak = false)

		onStateChanged()
	}

	fun onFocusModePreferenceChanged(packageName: String) {
		val spref = context.getSharedPreferences("appLists", 0)
		val st = spref.getStringSet("focus_mode", null)
		if (st == null) {
			BUG("st == null")
			return
		}

		setFocusModeStateForPkgInternal(packageName, isFocusModeEnabled && st.contains(packageName) && !isFocusModeBreak, isFocusModeEnabled && isFocusModeBreak, false)
	}

	fun takeFocusModeBreakWithDialog(activityContext: Activity, endActivity: Boolean, packageNames: Array<String>?) {
		val optionsS: Array<String> = Arrays.stream(breakTimeOptions).mapToObj { i ->
			context.resources.getQuantityString(R.plurals.break_mins, i, i)
		}.toArray { arrayOfNulls<String>(it) }
		val b = AlertDialog.Builder(activityContext)
			.setTitle(R.string.focus_mode_break)
			.setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
			.setItems(optionsS) { _, i ->
				val breakMins = breakTimeOptions[i]
				takeFocusModeBreak(packageNames, breakMins)
				if (endActivity) activityContext.finish()
			}
		b.show()
	}

	private val breakEndedCallback = Runnable { endFocusModeBreak(false) }

	@JvmOverloads
	fun endFocusModeBreak(needCancel: Boolean = true) {
		if (!isFocusModeEnabled) {
			BUG("Focus mode not active")
			return
		}
		if (!isFocusModeBreak) {
			BUG("No focus mode break active")
			return
		}

		if (needCancel) {
			handler.removeCallbacks(breakEndedCallback)
		}

		isFocusModeBreak = false

		val spref = context.getSharedPreferences("appLists", 0)
		val st = spref.getStringSet("focus_mode", null)
		if (st == null) {
			BUG("st == null")
			return
		}

		for (packageName in st) {
			setFocusModeStateForPkgInternal(packageName, suspend = true, forBreak = true, forAppBreak = false)
		}

		onStateChanged()
	}

	private val oneAppUnsuspendCallbacks = ArrayList<Runnable>()

	fun takeFocusModeBreak(packageNames: Array<String>?, breakMins: Int) {
		if (packageNames == null) {
			takeFocusModeBreak(breakMins)
			return
		}
		if (!isFocusModeEnabled) {
			BUG("Focus mode not active")
			return
		}
		if (isFocusModeBreak) {
			BUG("Focus mode break active")
			return
		}

		for (packageName in packageNames) {
			setFocusModeStateForPkgInternal(packageName, suspend = false, forBreak = true, forAppBreak = true)
		}
		val r = object : Runnable {
			override fun run() {
				oneAppUnsuspendCallbacks.remove(this)
				for (packageName in packageNames) {
					setFocusModeStateForPkgInternal(packageName, isFocusModeEnabled, isFocusModeEnabled, true)
				}
			}
		}
		oneAppUnsuspendCallbacks.add(r)
		handler.postDelayed(r, breakMins * 60 * 1000L)

		onStateChanged()
	}

	fun takeFocusModeBreak(breakMins: Int) {
		if (!isFocusModeEnabled) {
			BUG("Focus mode not active")
			return
		}
		if (isFocusModeBreak) {
			BUG("Focus mode break active")
			return
		}
		val spref = context.getSharedPreferences("appLists", 0)
		val st = spref.getStringSet("focus_mode", null)
		if (st == null) {
			BUG("st == null")
			return
		}

		isFocusModeBreak = true

		for (packageName in st) {
			setFocusModeStateForPkgInternal(packageName, suspend = false, forBreak = true, forAppBreak = false)
		}

		handler.postDelayed(breakEndedCallback, breakMins * 60 * 1000L)

		onStateChanged()
	}

	fun manualSuspend(packageNamesI: Array<String>?) {
		val packageNames: Array<String> = if (packageNamesI == null) {
			val spref = context.getSharedPreferences("appLists", 0)
			val packageNamesT = spref.getStringSet("manual_suspend", null)
			if (packageNamesT == null) {
				BUG("packagesNames == null")
				return
			}
			packageNamesT.toTypedArray()
		} else packageNamesI

		for (s in packageNames) {
			perAppState[s] = perAppState.getOrDefault(s, 0) or State.STATE_MANUAL_SUSPEND
			updateSuspendStatusForApp(s)
		}

		onStateChanged()
	}

	fun manualUnsuspend(packageNamesI: Array<String>?) {
		val packageNames: Array<String> = if (packageNamesI == null) {
			val spref = context.getSharedPreferences("appLists", 0)
			val packageNamesT = spref.getStringSet("manual_suspend", null)
			if (packageNamesT == null) {
				BUG("packagesNames == null")
				return
			}
			packageNamesT.toTypedArray()
		} else packageNamesI

		for (s in packageNames) {
			perAppState[s] = perAppState.getOrDefault(s, 0) and State.STATE_MANUAL_SUSPEND.inv()
			updateSuspendStatusForApp(s)
		}

		onStateChanged()
	}
}