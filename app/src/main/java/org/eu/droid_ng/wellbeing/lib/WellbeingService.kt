package org.eu.droid_ng.wellbeing.lib

import android.app.Activity
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.Wellbeing
import org.eu.droid_ng.wellbeing.broadcast.AppTimersBroadcastReciever
import org.eu.droid_ng.wellbeing.broadcast.NotificationBroadcastReciever
import org.eu.droid_ng.wellbeing.join
import org.eu.droid_ng.wellbeing.lib.BugUtils.Companion.BUG
import org.eu.droid_ng.wellbeing.lib.Utils.getTimeUsed
import org.eu.droid_ng.wellbeing.prefs.MainActivity
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate.SuspendDialogInfo
import org.eu.droid_ng.wellbeing.ui.TakeBreakDialogActivity
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.stream.Collectors


class WellbeingService(private val context: Context) {
	private var host: WellbeingStateHost? = null

	fun bindToHost(newhost: WellbeingStateHost?) {
		host = newhost
		if (host != null) {
			onServiceStartedCallbacks.toTypedArray().forEach {
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
		const val INTENT_ACTION_QUIT_BED = "org.eu.droid_ng.wellbeing.QUIT_BED"
		const val INTENT_ACTION_QUIT_FOCUS = "org.eu.droid_ng.wellbeing.QUIT_FOCUS"
		const val INTENT_ACTION_UNSUSPEND_ALL = "org.eu.droid_ng.wellbeing.UNSUSPEND_ALL"
		@JvmField val breakTimeOptions = intArrayOf(1, 3, 5, 10, 15) // keep in sync with getUseAppForString
	}

	private val handler = Handler.createAsync(context.mainLooper)
	private val pm = context.packageManager
	private val pmd = PackageManagerDelegate(pm)
	val cdm: PackageManagerDelegate.IColorDisplayManager = PackageManagerDelegate.getColorDisplayManager(context)
	@JvmField val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

	private val oidMap = context.getSharedPreferences("AppTimersInternal", 0)
	private val config = context.getSharedPreferences("appTimers", 0)
	private val sched = context.getSharedPreferences("sched", 0)

	@JvmField var focusModeAllApps = true
	@JvmField var focusModeBreakTimeDialog = -1
	@JvmField var focusModeBreakTimeNotification = -1
	@JvmField var manualSuspendDialog = true
	@JvmField var manualSuspendAllApps = true
	@JvmField var appTimerDialogBreakTime = -1

	private var triggers: Set<Trigger> = HashSet()
	private var conditions: Set<Condition> = HashSet()

	private fun loadSettings() {
		val prefs = context.getSharedPreferences("service", 0)
		focusModeBreakTimeNotification = Integer.parseInt(prefs.getString("focus_notification", focusModeBreakTimeNotification.toString()) ?: focusModeBreakTimeNotification.toString())
		focusModeBreakTimeDialog = Integer.parseInt(prefs.getString("focus_dialog", focusModeBreakTimeDialog.toString()) ?: focusModeBreakTimeDialog.toString())
		manualSuspendDialog = prefs.getBoolean("manual_dialog", manualSuspendDialog)
		manualSuspendAllApps = prefs.getBoolean("manual_all", manualSuspendAllApps)
		focusModeAllApps = prefs.getBoolean("focus_all", focusModeAllApps)
		appTimerDialogBreakTime = Integer.parseInt(prefs.getString("app_timer_dialog", appTimerDialogBreakTime.toString()) ?: appTimerDialogBreakTime.toString())

		loadSchedcfg()
	}

	private fun loadSchedcfg() {
		sched.getStringSet("triggers", HashSet())?.stream()?.map { ScheduleUtils.triggerFromString(it) }?.collect(Collectors.toSet())?.let { triggers = it }
		sched.getStringSet("conditions", HashSet())?.stream()?.map { ScheduleUtils.conditionFromString(it) }?.collect(Collectors.toSet())?.let { conditions = it }
		ensureSchedSetup()
	}

	private fun writeSchedcfg() {
		val s = sched.edit().clear()

		s.putStringSet("triggers", triggers.stream().map { it.toString(";;") }.collect(Collectors.toSet()))
		s.putStringSet("conditions", conditions.stream().map { it.toString(";;") }.collect(Collectors.toSet()))

		s.apply()
	}

	init {
		Utils.clearUsageStatsCache(usm, true)
		loadSettings()

		context.registerReceiver(object : BroadcastReceiver() {
			override fun onReceive(p0: Context?, p1: Intent?) {
				onUpdatePowerConnection()
			}
		}, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
	}

	private var bedtimeModeEnabled = false
	private var isFocusModeEnabled = false
	private var isFocusModeBreak /* global break */ = false
	private val perAppState: HashMap<String /* packageName */, Int /* does NOT contain global flags like FOCUS_MODE_ENABLED or FOCUS_MODE_GLOBAL_BREAK, so always use getAppState() when reading */> = HashMap()

	// Service / Global state. Do not confuse with per-app state, that's using the same values.
	@JvmOverloads
	fun getState(includeAppState: Boolean = true): State {
		val value =
			(if (bedtimeModeEnabled) State.STATE_BED_MODE else 0) or
			(if (isFocusModeEnabled) State.STATE_FOCUS_MODE_ENABLED else 0) or
			(if (isFocusModeBreak) State.STATE_FOCUS_MODE_GLOBAL_BREAK else 0) or
			(if (includeAppState && (perAppState.entries.stream().filter { (it.value and State.STATE_FOCUS_MODE_APP_BREAK) > 0 }.findAny().isPresent)) State.STATE_FOCUS_MODE_APP_BREAK else 0) or
			(if (includeAppState && (perAppState.entries.stream().filter { (it.value and State.STATE_MANUAL_SUSPEND) > 0 }.findAny().isPresent)) State.STATE_MANUAL_SUSPEND else 0) or
			(if (includeAppState && (perAppState.entries.stream().filter { (it.value and State.STATE_APP_TIMER_SET) > 0 }.findAny().isPresent)) State.STATE_APP_TIMER_SET else 0) or
			(if (includeAppState && (perAppState.entries.stream().filter { (it.value and State.STATE_APP_TIMER_EXPIRED) > 0 }.findAny().isPresent)) State.STATE_APP_TIMER_EXPIRED else 0) or
			(if (includeAppState && (perAppState.entries.stream().filter { (it.value and State.STATE_APP_TIMER_BREAK) > 0 }.findAny().isPresent)) State.STATE_APP_TIMER_BREAK else 0)

		return State(value)
	}

	fun getAppState(packageName: String): State {
		var value = perAppState.getOrDefault(packageName, 0)

		/* apply matching global flags */
		val global = getState(false).toInt()
		if ((value and State.STATE_FOCUS_MODE_ENABLED) > 0) {
			value = value or (global and State.STATE_FOCUS_MODE_GLOBAL_BREAK)
		}

		/* apply app timer flags */
		if (config.getInt(packageName, -1) > 0) {
			value = value or State.STATE_APP_TIMER_SET
		}
		if ((value and State.STATE_APP_TIMER_SET) > 0 && Duration.ofMinutes(config.getInt(packageName, 0).toLong()).minus(getTimeUsed(usm, arrayOf(packageName))).toMinutes() <= 0) {
			value = value or State.STATE_APP_TIMER_EXPIRED
		}
		if ((value and State.STATE_APP_TIMER_SET) > 0 && oidMap.contains(ParsedUoid("AppBreak", 0, arrayOf(packageName)).toString())) {
			value = value or State.STATE_APP_TIMER_BREAK
		}

		return State(value)
	}

	fun setBedtimeMode(enable: Boolean) {
		bedtimeModeEnabled = enable

		val prefs = context.getSharedPreferences("bedtime_mode", 0)
		if (enable) {
			if (prefs.getBoolean("greyscale", false)) {
				cdm.setSaturationLevel(0)
			}
		} else {
			if (prefs.getBoolean("greyscale", false)) {
				cdm.setSaturationLevel(100)
			}
		}

		onStateChanged()
	}

	fun onAppTimerExpired(observerId: Int, uniqueObserverId: String) {
		var msg: String
		var uoid: String = uniqueObserverId
		if (oidMap.getInt(uoid, -2) != observerId) {
			msg = "Warning: unknown oid/uoid - $observerId / $uoid - this might be an bug? Trying to recover."
			//Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show(); this should really be shown. but the underlying problem lies in android code :(
			Log.e("AppTimersInternal", msg)
			// Attempt to recover, in doubt always trust the oid. Because android is fucking dumb. Thank you.
			uoid = oidMap.all.entries.stream().filter { a -> observerId == a.value }
				.findAny().get().key
		}

		val parsed = ParsedUoid.from(uoid)
		msg = "AppTimersInternal: success oid:" + observerId + " action:" + parsed.action + " timeMillis:" + parsed.timeMillis + " pkgs:" + String.join(",", parsed.pkgs)
		Log.i("AppTimersInternal", msg)

		when (parsed.action) {
			"AppTimer", "AppLimit" -> {
				dropAppTimer(parsed)
				parsed.pkgs.forEach {
					if (it != null) {
						updateSuspendStatusForApp(it)
					}
				}
			}
			"AppBreak" -> endBreak(parsed.pkgs)
			else -> {
				Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
				dropAppTimer(parsed)
			}
		}
	}

	private fun endBreak(pkgs: Array<String?>) {
		val u = ParsedUoid("AppBreak", 0, pkgs)
		if (!oidMap.contains(u.toString())) return
		Log.i("AppTimersInternal", "end break for " + pkgs.contentToString())
		dropAppTimer(u)
		pkgs.forEach {
			if (it != null) {
				updateSuspendStatusForApp(it)
			}
		}
	}

	private fun loadAppTimer(packageName: String) {
		val s = arrayOf<String?>(packageName)
		val i = config.getInt(packageName, -1)
		val m = Duration.ofMinutes(i.toLong()).minus(getTimeUsed(usm, s))
		if (i > 0 && m.toMinutes() > 0)
			setAppTimer(s, m, getTimeUsed(usm, s))
		updateSuspendStatusForApp(packageName)
	}

	private fun takeAppTimerBreak(packageNames: Array<String?>, breakMins: Int) {
		val u = ParsedUoid("AppBreak", 0, packageNames).toString()
		if (!oidMap.contains(u)) {
			updatePrefs(u, makeOid())
		}
		setAppTimerInternal(u, packageNames, Duration.ofMinutes(breakMins.toLong()), null)
		packageNames.forEach {
			if (it != null) {
				updateSuspendStatusForApp(it)
			}
		}
	}

	fun takeAppTimerBreakWithDialog(activityContext: Activity, endActivity: Boolean, packageNames: Array<String?>) {
		val optionsS: Array<String> = Arrays.stream(breakTimeOptions).mapToObj { i ->
			context.resources.getQuantityString(R.plurals.break_mins, i, i)
		}.toArray { arrayOfNulls<String>(it) }
		val b = AlertDialog.Builder(activityContext)
			.setTitle(R.string.focus_mode_break)
			.setNegativeButton(R.string.cancel) { d, _ -> d.dismiss() }
			.setItems(optionsS) { _, i ->
				val breakMins = breakTimeOptions[i]
				takeAppTimerBreak(packageNames, breakMins)
				if (endActivity) activityContext.finish()
			}
		b.show()
	}

	private fun loadAppTimers() {
		oidMap.edit().clear().apply()
		for (pkg in config.all.keys) {
			loadAppTimer(pkg)
		}
	}

	fun onUpdateAppTimerPreference(pkgName: String, oldLimit: Duration) {
		val s = arrayOf<String?>(pkgName)
		var u = ParsedUoid("AppTimer", oldLimit.toMillis(), s)
		if (oidMap.contains(u.toString())) dropAppTimer(u)
		u = ParsedUoid("AppLimit", oldLimit.toMillis(), s)
		if (oidMap.contains(u.toString())) dropAppTimer(u)
		u = ParsedUoid("AppBreak", 0, s)
		if (oidMap.contains(u.toString())) dropAppTimer(u)
		loadAppTimer(pkgName)
	}

	fun onBootCompleted() {
		loadAppTimers()
	}

	private fun updateServiceStatus() {
		loadSettings()
		val state = getState()
		val needServiceRunning = state.isFocusModeEnabled() || state.isSuspendedManually() || state.isBedtimeModeEnabled()
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
			} else if (state.isBedtimeModeEnabled()) {
				host?.updateNotification(
					R.string.bedtime_mode,
					R.string.bedtime_desc,
					R.drawable.baseline_bedtime_24,
					arrayOf(
						host?.buildAction(
							R.string.disable, R.drawable.baseline_cancel_24, Intent(
								context,
								NotificationBroadcastReciever::class.java
							).setAction(INTENT_ACTION_QUIT_BED), true
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

	private fun getUseAppForString(time: Int): Int {
		// keep in sync with breakTimeOptions
		return when (time) {
			1 -> R.string.break_dialog_1
			3 -> R.string.break_dialog_3
			5 -> R.string.break_dialog_5
			10 -> R.string.break_dialog_10
			15 -> R.string.break_dialog_15
			else -> {
				throw IllegalArgumentException("$time needs to be in breakTimeOptions list")
			}
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
			INTENT_ACTION_QUIT_BED -> {
				setBedtimeMode(false)
			}
			else -> {
				BUG("invalid notification action: $action")
			}
		}
	}

	private fun updateSuspendStatusForApp(packageName: String) {
		val state = getAppState(packageName)
		val f: Array<String> = if (state.isFocusModeEnabled() && !(state.isOnFocusModeBreakGlobal() || state.isOnFocusModeBreakPartial())) {
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
				.setNeutralButtonText(if (focusModeBreakTimeDialog == -1) R.string.dialog_btn_settings else getUseAppForString(focusModeBreakTimeDialog))
				.setNeutralButtonAction(if (focusModeBreakTimeDialog == -1) SuspendDialogInfo.BUTTON_ACTION_MORE_DETAILS else SuspendDialogInfo.BUTTON_ACTION_UNSUSPEND)
				.build()
			pmd.setPackagesSuspended(arrayOf(packageName), true, null, null, di)
		} else if (state.isSuspendedManually()) {
			val di = SuspendDialogInfo.Builder()
				.setTitle(R.string.dialog_title)
				.setMessage(R.string.dialog_message)
				.setIcon(R.drawable.ic_baseline_app_blocking_24)
				.setNeutralButtonText(if (!manualSuspendDialog) R.string.dialog_btn_settings else (if (manualSuspendAllApps) R.string.unsuspend_all else R.string.unsuspend))
				.setNeutralButtonAction(if (!manualSuspendDialog) SuspendDialogInfo.BUTTON_ACTION_MORE_DETAILS else SuspendDialogInfo.BUTTON_ACTION_UNSUSPEND)
				.build()
			pmd.setPackagesSuspended(arrayOf(packageName), true, null, null, di)
		} else if (state.isAppTimerExpired() && !state.isAppTimerBreak()) {
			pmd.setPackagesSuspended(
				arrayOf(packageName), true, null, null, SuspendDialogInfo.Builder()
					.setTitle(R.string.app_timers)
					.setMessage(context.getString(R.string.app_timer_exceed_f, pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0))))
					.setNeutralButtonText(if (appTimerDialogBreakTime == -1) R.string.dialog_btn_settings else getUseAppForString(appTimerDialogBreakTime))
					.setNeutralButtonAction(if (appTimerDialogBreakTime == -1) SuspendDialogInfo.BUTTON_ACTION_MORE_DETAILS else SuspendDialogInfo.BUTTON_ACTION_UNSUSPEND)
					.setIcon(R.drawable.ic_focus_mode).build()
			)
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

	private fun takeFocusModeBreak(packageNames: Array<String>?, breakMins: Int) {
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


	// start time limit core
	private fun updatePrefs(key: String, value: Int) {
		if (value < 0) {
			oidMap.edit().remove(key).apply()
		} else {
			oidMap.edit().putInt(key, value).apply()
		}
	}

	private fun makeOid(): Int {
		val vals: Collection<*> = oidMap.all.values
		// try to save time by starting at size value
		for (i in vals.size..999) {
			if (!vals.contains(i)) return i
		}
		// if all high values are used up, try all values
		for (i in 0..999) {
			if (!vals.contains(i)) return i
		}
		throw IllegalStateException("more than 1000 observers registered")
	}

	private class ParsedUoid(val action: String, val timeMillis: Long, val pkgs: Array<String?>) {
		override fun toString(): String {
			return action + ":" + timeMillis + "//" + java.lang.String.join(":", *pkgs)
		}

		companion object {
			fun from(uoid: String): ParsedUoid {
				val l = uoid.indexOf(":")
				val ll = uoid.indexOf("//")
				val action = uoid.substring(0, l)
				val timeMillis = uoid.substring(l + 1, ll).toLong()
				val pkgs: Array<String?> =
					uoid.substring(ll + 2).split(":".toRegex()).dropLastWhile { it.isEmpty() }
						.toTypedArray()
				return ParsedUoid(action, timeMillis, pkgs)
			}
		}
	}

	private fun setUnhintedAppTimerInternal(
		oid: Int,
		uoid: String,
		toObserve: Array<String?>,
		timeLimit: Duration
	) {
		val i = Intent(context, AppTimersBroadcastReciever::class.java)
		i.putExtra("observerId", oid)
		i.putExtra("uniqueObserverId", uoid)
		val pintent: PendingIntent =
			PendingIntent.getBroadcast(context, oid, i, PendingIntent.FLAG_IMMUTABLE)
		PackageManagerDelegate.registerAppUsageObserver(
			usm,
			oid,
			toObserve,
			timeLimit.toMillis(),
			TimeUnit.MILLISECONDS,
			pintent
		)
	}

	private fun setHintedAppTimerInternal(
		oid: Int,
		uoid: String,
		toObserve: Array<String?>,
		timeLimit: Duration,
		timeUsed: Duration
	) {
		val i = Intent(context, AppTimersBroadcastReciever::class.java)
		i.putExtra("observerId", oid)
		i.putExtra("uniqueObserverId", uoid)
		val pintent: PendingIntent =
			PendingIntent.getBroadcast(context, oid, i, PendingIntent.FLAG_IMMUTABLE)
		PackageManagerDelegate.registerAppUsageLimitObserver(
			usm,
			oid,
			toObserve,
			timeLimit,
			timeUsed,
			pintent
		)
	}

	private fun setAppTimerInternal(
		uoid: String,
		toObserve: Array<String?>,
		timeLimit: Duration,
		timeUsed: Duration?
	) {
		val oid: Int = oidMap.getInt(uoid, -1)
		if (timeUsed == null) {
			setUnhintedAppTimerInternal(oid, uoid, toObserve, timeLimit)
		} else {
			setHintedAppTimerInternal(oid, uoid, toObserve, timeLimit, timeUsed)
		}
	}

	private fun dropAppTimer(parsedUoid: ParsedUoid) {
		val uoid = parsedUoid.toString()
		updatePrefs(uoid, -1) //delete pref
		if (parsedUoid.action != "AppLimit") {
			PackageManagerDelegate.unregisterAppUsageLimitObserver(usm, oidMap.getInt(uoid, -1))
		} else {
			PackageManagerDelegate.unregisterAppUsageObserver(usm, oidMap.getInt(uoid, -1))
		}
	}

	private fun setAppTimer(
		toObserve: Array<String?>,
		timeLimit: Duration,
		timeUsed: Duration?
	) {
		// AppLimit: do not provide info to launcher, use registerAppUsageObserver
		// AppTimer: provide info to launcher, use registerAppUsageLimitObserver
		val uoid = ParsedUoid(
			if (timeUsed == null) "AppLimit" else "AppTimer",
			timeLimit.toMillis(),
			toObserve
		).toString()
		var timeLimitInternal = timeLimit
		if (timeUsed != null) {
			timeLimitInternal = timeLimitInternal.minus(timeUsed)
		}
		if (!oidMap.contains(uoid)) {
			updatePrefs(uoid, makeOid())
		}
		setAppTimerInternal(uoid, toObserve, timeLimitInternal, timeUsed)
	}
	// end time limit core

	fun onUpdatePowerConnection() {
		val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { context.registerReceiver(null, it) }

		val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
		val charging = chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
				chargePlug == BatteryManager.BATTERY_PLUGGED_AC

		doTrigger(!charging) { it is ChargerTriggerCondition }
	}

	private fun ensureSchedSetup() {
		triggers.forEach { it.setup(context, this) }
	}

	private fun triggerFired(expire: Boolean, trigger: Trigger) {
		when (val id = trigger.id.split("|")[0]) {
			"bedtime_mode" -> {
				if (expire && bedtimeModeEnabled) {
					setBedtimeMode(false)
				} else if (!expire) {
					setBedtimeMode(true)
				}
			}
			"focus_mode" -> {
				if (expire && isFocusModeEnabled) {
					disableFocusMode()
				} else if (!expire) {
					enableFocusMode()
				}
			}
			else -> {
				BUG("invalid trigger id $id expire=$expire")
			}
		}
	}

	private fun doTrigger(expire: Boolean, condition: (Trigger) -> Boolean) {
		triggers.forEach { fired ->
			val t = if (condition(fired)) {
				fired
			} else if (fired is Container) {
				fired.onTrigger(condition)
			} else {
				null
			}
			t?.let { trigger ->
				var isOk = true
				if (!expire) {
					conditions.forEach {
						if (trigger.id == it.id || trigger.id.startsWith("${it.id}|")) {
							isOk = isOk && it.isFulfilled(context, this)
						}
					}
				}
				if (isOk) {
					triggerFired(expire, trigger)
				}
			}
		}
	}

	fun onAlarmFired(id: String) {
		var t = false
		val nid = if (id.startsWith("expire::")) {
			t = true
			id.substring(8)
		} else id
		doTrigger(t) { it is TimeTriggerCondition && (nid == it.id || nid.startsWith("${it.id}|")) }
	}

	fun setTriggersForId(id: String, triggersIn: Array<out Trigger>) {
		triggers.filter { (id == it.id || it.id.startsWith("${id}|")) }.forEach { it.dispose(context, this) }
		triggers = triggers.filterNot { (id == it.id || it.id.startsWith("${id}|")) }.toSet().plus(triggersIn)
		ensureSchedSetup()
	}

	fun setConditionsForId(id: String, conditionsIn: Array<out Condition>) {
		conditions = conditions.filterNot { (id == it.id || it.id.startsWith("${id}|")) }.toSet().plus(conditionsIn)
	}

	fun getTriggersForId(id: String): List<Trigger> {
		return triggers.filter { (id == it.id || it.id.startsWith("${id}|")) }
	}

	fun getConditionsForId(id: String): List<Condition> {
		return conditions.filter { (id == it.id || it.id.startsWith("${id}|")) }
	}

	fun setTriggerConditionForId(id: String, triggerConditions: Array<TriggerCondition>) {
		Log.e("AAAAA", triggerConditions.contentDeepToString())
		setTriggersForId(id, triggerConditions)
		setConditionsForId(id, triggerConditions)
		writeSchedcfg()
	}

	fun getTriggerConditionForId(id: String): List<TriggerCondition> {
		return conditions.filter { (id == it.id || it.id.startsWith("${id}|")) && it is TriggerCondition }.map { it as TriggerCondition }
			.plus(triggers.filter { (id == it.id || it.id.startsWith("${id}|")) && it is TriggerCondition }.map { it as TriggerCondition }).distinct().also {
				Log.e("BBBBB", it.toTypedArray().contentDeepToString())
			}
	}
}