package org.eu.droid_ng.wellbeing.lib

import android.app.*
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.Wellbeing
import org.eu.droid_ng.wellbeing.broadcast.AppTimersBroadcastReceiver
import org.eu.droid_ng.wellbeing.broadcast.NotificationBroadcastReceiver
import org.eu.droid_ng.wellbeing.join
import org.eu.droid_ng.wellbeing.lib.BugUtils.Companion.BUG
import org.eu.droid_ng.wellbeing.lib.Utils.getTimeUsed
import org.eu.droid_ng.wellbeing.prefs.MainActivity
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate.SuspendDialogInfo
import org.eu.droid_ng.wellbeing.ui.TakeBreakDialogActivity
import org.eu.droid_ng.wellbeing.widget.ScreenTimeAppWidget
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.stream.Collectors


class WellbeingService(private val context: Context) {
	private var host: WellbeingStateHost? = null
	// systemApp should always be true, only used for development purposes.
	private val systemApp: Boolean = (context.applicationInfo.flags and
			(ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM)) > 1
	private val frameworkService: WellbeingFrameworkService =
			WellbeingFrameworkService(context, this)

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


	@JvmOverloads
	fun getInstalledApplications(flags: Int = 0): List<ApplicationInfo> {
		val newflags = (when(systemApp) {
			true -> Utils.PACKAGE_MANAGER_MATCH_INSTANT
			false -> 0
		} or flags)
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(newflags.toLong()))
		} else {
			@Suppress("deprecation")
			pm.getInstalledApplications(newflags)
		}
	}

	@JvmOverloads
	@Throws(PackageManager.NameNotFoundException::class)
	fun getApplicationInfo(packageName: String, matchUninstalled: Boolean = true, flags: Int = 0): ApplicationInfo {
		val newflags = when(matchUninstalled) {
			true -> PackageManager.MATCH_UNINSTALLED_PACKAGES
			false -> 0
		} or when(systemApp) {
			true -> Utils.PACKAGE_MANAGER_MATCH_INSTANT
			false -> 0
		} or PackageManager.MATCH_ALL or flags
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(newflags.toLong()))
		} else {
			@Suppress("deprecation")
			pm.getApplicationInfo(packageName, newflags)
		}
	}

	@JvmOverloads
	@Throws(PackageManager.NameNotFoundException::class)
	fun getApplicationLabel(packageName: String, matchUninstalled: Boolean = true): CharSequence {
		return pm.getApplicationLabel(getApplicationInfo(packageName, matchUninstalled))
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
	private val alc = AlarmCoordinator(context)
	private val notificationManager = context.getSystemService(NotificationManager::class.java) as NotificationManager

	private val oidMap = context.getSharedPreferences("AppTimersInternal", 0)
	private val config = context.getSharedPreferences("appTimers", 0)
	private val sched = context.getSharedPreferences("sched", 0)

	@JvmField var focusModeAllApps = true
	@JvmField var focusModeBreakTimeDialog = -1
	@JvmField var focusModeBreakTimeNotification = -1
	@JvmField var manualSuspendDialog = true
	@JvmField var manualSuspendAllApps = true
	@JvmField var appTimerDialogBreakTime = -1
	private var reminderMin = -1
	private var airplaneState: WellbeingAirplaneState
	private var airplaneStateLogical: Boolean = false

	private var triggers: Set<Trigger> = HashSet()

	private fun loadSettings() {
		val prefs = context.getSharedPreferences("service", 0)
		focusModeBreakTimeNotification = Integer.parseInt(prefs.getString("focus_notification", focusModeBreakTimeNotification.toString()) ?: focusModeBreakTimeNotification.toString())
		focusModeBreakTimeDialog = Integer.parseInt(prefs.getString("focus_dialog", focusModeBreakTimeDialog.toString()) ?: focusModeBreakTimeDialog.toString())
		manualSuspendDialog = prefs.getBoolean("manual_dialog", manualSuspendDialog)
		manualSuspendAllApps = prefs.getBoolean("manual_all", manualSuspendAllApps)
		focusModeAllApps = prefs.getBoolean("focus_all", focusModeAllApps)
		appTimerDialogBreakTime = Integer.parseInt(prefs.getString("app_timer_dialog", appTimerDialogBreakTime.toString()) ?: appTimerDialogBreakTime.toString())
		reminderMin = Integer.parseInt(prefs.getString("app_timer_reminder", reminderMin.toString()) ?: reminderMin.toString())

		loadSchedcfg()
		alc.updateState()
	}

	private fun loadSchedcfg() {
		sched.getStringSet("triggers", HashSet())?.stream()?.map { raw ->
			val values = raw.split(";;")
			if (values.size < 2) throw IllegalStateException("invalid value $raw")
			return@map when (values[0]) {
				"time" -> {
					val bools = BooleanArray(7); for (i in bools.indices) if (values[8].toInt() and (1 shl i) != 0) bools[i] = true // bitmask -> boolean[]
					TimeChargerTriggerCondition(values[1], values[2], values[3].toBooleanStrict(), values[4].toInt(), values[5].toInt(), values[6].toInt(), values[7].toInt(), bools, values[9].toBooleanStrict(), values[10].toBooleanStrict())
				}
				else -> {
					throw IllegalStateException("invalid trigger type ${values[0]}")
				}
			}
		}?.collect(Collectors.toSet())?.let { triggers = it }

		ensureSchedSetup()
	}

	private fun writeSchedcfg() {
		val s = sched.edit().clear()

		s.putStringSet("triggers", triggers.stream().map {
			when (it) {
				is TimeChargerTriggerCondition -> {
					var bits = 0; for (i in 0 until it.weekdays.size) if (it.weekdays[i]) bits = bits or (1 shl i) // boolean[] -> bitmask
					"time;;${it.id};;${it.iid};;${it.enabled};;${it.startHour};;${it.startMinute};;${it.endHour};;${it.endMinute};;${bits};;${it.needCharger};;${it.endOnAlarm}"
				}
				else -> throw IllegalStateException("unknown trigger ${it::class.qualifiedName}")
			}
		}.collect(Collectors.toSet()))

		s.apply()
	}

	fun updateWidget(widget: Class<out AppWidgetProvider>) {
		val intent = Intent(context, widget)
		intent.action = "org.eu.droid_ng.wellbeing.APPWIDGET_UPDATE"
		context.sendBroadcast(intent)
	}

	private var bedtimeModeEnabled = false
	private var isFocusModeEnabled = false
	private var isFocusModeBreak /* global break */ = false
	private val perAppState: HashMap<String /* packageName */, Int /* does NOT contain global flags like FOCUS_MODE_ENABLED or FOCUS_MODE_GLOBAL_BREAK, so always use getAppState() when reading */> = HashMap()

	init {
		Utils.clearUsageStatsCache(usm, pm, true)
		airplaneState = when(WellbeingAirplaneState.isAirplaneModeOn(context)) {
			true -> WellbeingAirplaneState.ENABLED_BY_SYSTEM
			false -> WellbeingAirplaneState.DISABLED_BY_SYSTEM
		}
		onStateChanged() // includes loadSettings()
		ScheduleUtils.ensureWidgetAlarmSet(context, handler, 60, ScreenTimeAppWidget::class.java)

		if (notificationManager.getNotificationChannel("reminder") == null) {
			val name: CharSequence = context.getString(R.string.channel2_name)
			val description = context.getString(R.string.channel2_description)
			val importance = NotificationManager.IMPORTANCE_HIGH
			val channel = NotificationChannel("reminder", name, importance)
			channel.description = description
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				channel.isBlockable = true
			}
			notificationManager.createNotificationChannel(channel)
		}

		context.registerReceiver(object : BroadcastReceiver() {
			override fun onReceive(p0: Context?, p1: Intent?) {
				onUpdatePowerConnection()
			}
		}, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
		context.registerReceiver(object : BroadcastReceiver() {
			override fun onReceive(p0: Context?, p1: Intent?) {
				airplaneState = if (WellbeingAirplaneState.isAirplaneModeOn(context)) {
					airplaneState.onReceiveAirplaneEnabled()
				} else {
					airplaneState.onReceiveAirplaneDisabled()
				}
			}
		}, IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED))
		frameworkService.tryConnect()
	}

	fun onWellbeingFrameworkConnected(initial: Boolean) {
		if (hasWellbeingAirplaneModeCapabilities()) {
			if (airplaneState.wellbeingAirplaneModeState != airplaneStateLogical) {
				setWellbeingAirplaneMode(airplaneStateLogical)
			} else if (initial) {
				val prefs = context.getSharedPreferences("restore_state", 0)
				if (prefs.getBoolean("restore_airplane_mode", false) &&
						!airplaneState.wellbeingAirplaneModeState) {
					prefs.edit().remove("restore_airplane_mode").apply()
					frameworkService.setAirplaneMode(false)
				}
			}
		}
	}

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
		if ((value and State.STATE_APP_TIMER_SET) > 0 && Duration.ofMinutes(config.getInt(packageName, 0).toLong()).minus(getTimeUsed(usm, packageName)).toMinutes() <= 0) {
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

		setWellbeingAirplaneMode(enable &&
				prefs.getBoolean("airplane_mode", false))

		onStateChanged()

		doUpdateTile(BedtimeModeQSTile::class.java)
	}

	private fun hasWellbeingAirplaneModeCapabilities(): Boolean {
		return frameworkService.versionCode() >= 1
	}

	fun setWellbeingAirplaneMode(enable: Boolean) {
		airplaneStateLogical = enable
		val oldState = airplaneState
		if (!hasWellbeingAirplaneModeCapabilities()) {
			// Allow partial update when in state that
			// previously had airplane mode capabilities
			if (oldState.wellbeingAirplaneModeState && !enable) {
				airplaneState = airplaneState.onDisableAirplaneByWellbeing()
				if (oldState.shouldRestoreAirplaneMode() !=
						airplaneState.shouldRestoreAirplaneMode()) {
					val prefs = context.getSharedPreferences("restore_state", 0)
					prefs.edit().putBoolean("restore_airplane_mode",
							airplaneState.shouldRestoreAirplaneMode()).apply()
				}
			}
			return
		}
		airplaneState = when(enable) {
			true -> airplaneState.onEnableAirplaneByWellbeing()
			false -> airplaneState.onDisableAirplaneByWellbeing()
		}
		if (airplaneState.airplaneModeState !=
				oldState.airplaneModeState) {
			frameworkService.setAirplaneMode(
					airplaneState.airplaneModeState)
		}
		if (airplaneState.shouldRestoreAirplaneMode()
				!= oldState.shouldRestoreAirplaneMode()) {
			val prefs = context.getSharedPreferences("restore_state", 0)
			prefs.edit().putBoolean("restore_airplane_mode",
					airplaneState.shouldRestoreAirplaneMode()).apply()
		}
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
					if (it == null) return@forEach
					updateSuspendStatusForApp(it)
				}
			}
			"Reminder" -> {
				dropAppTimer(parsed)
				parsed.pkgs.forEach {
					if (it == null) return@forEach
					val text = context.getString(
						R.string.app_timer_reminder_title,
						reminderMin
					)
					val n = Notification.Builder(context, "reminder")
						.setWhen(System.currentTimeMillis())
						.setSmallIcon(R.drawable.ic_focus_mode)
						.setOnlyAlertOnce(true)
						.setContentTitle(text)
						.setTicker(text)
						.setContentText(
							context.getString(
								R.string.app_timer_reminder,
								getApplicationLabel(it)
							)
						)
					notificationManager.notify(
						(System.currentTimeMillis() / 1000).toInt(),
						n.build()
					)
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
		dropAppTimer(u)
		pkgs.forEach {
			if (it == null) return@forEach
			updateSuspendStatusForApp(it)
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
		setAppTimerInternal(u, packageNames, Duration.ofMinutes(breakMins.toLong()), getTimeUsed(usm, packageNames))
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
		u = ParsedUoid("Reminder", 0, s)
		if (oidMap.contains(u.toString())) dropAppTimer(u)
		loadAppTimer(pkgName)
	}

	fun onBootCompleted() {
		// Try to reconnect to frameworkService if the first connection failed.
		frameworkService.tryConnect()
		loadAppTimers()
		doUpdateTile(FocusModeQSTile::class.java)
		doUpdateTile(BedtimeModeQSTile::class.java)
		onStateChanged()
	}

	private fun doUpdateTile(tile: Class<out TileService>) {
		TileService.requestListeningState(context, ComponentName(context, tile))
	}

	private fun updateServiceStatus() {
		loadSettings()
		updateWidget(ScreenTimeAppWidget::class.java)
		val state = getState()
		val needServiceRunning = state.isFocusModeEnabled() || state.isSuspendedManually() || state.isBedtimeModeEnabled()
		val next = {
			if (state.isFocusModeEnabled()) {
				if (state.isOnFocusModeBreakGlobal()) {
					host?.updateNotification(
						R.string.focus_mode,
						R.string.notification_focus_mode_break,
						R.drawable.outline_badge_24,
						arrayOf(
							host?.buildAction(
								R.string.focus_mode_break_end, R.drawable.ic_take_break, Intent(
									context,
									NotificationBroadcastReceiver::class.java
								).setAction(INTENT_ACTION_QUIT_BREAK), true
							),
							host?.buildAction(
								R.string.focus_mode_off, R.drawable.baseline_cancel_24, Intent(
									context,
									NotificationBroadcastReceiver::class.java
								).setAction(INTENT_ACTION_QUIT_FOCUS), true
							)
						),
						Intent(context, MainActivity::class.java)
					)
				} else {
					host?.updateNotification(
						R.string.focus_mode,
						R.string.notification_focus_mode,
						R.drawable.outline_badge_24,
						arrayOf(
							if (focusModeBreakTimeNotification == -1) host?.buildAction(
								R.string.focus_mode_break, R.drawable.ic_take_break, Intent(
									context,
									TakeBreakDialogActivity::class.java
								), false
							) else host?.buildAction(
								R.string.focus_mode_break, R.drawable.ic_take_break, Intent(
									context,
									NotificationBroadcastReceiver::class.java
								).setAction(INTENT_ACTION_TAKE_BREAK), true
							),
							host?.buildAction(
								R.string.focus_mode_off, R.drawable.baseline_cancel_24, Intent(
									context,
									NotificationBroadcastReceiver::class.java
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
					R.drawable.ic_baseline_person_24,
					arrayOf(
						host?.buildAction(
							R.string.unsuspend_all, R.drawable.baseline_exit_to_app_24, Intent(
								context,
								NotificationBroadcastReceiver::class.java
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
								NotificationBroadcastReceiver::class.java
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
				getApplicationLabel(packageName, false)
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
					.setMessage(context.getString(R.string.app_timer_exceed_f, getApplicationLabel(packageName)))
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

		doUpdateTile(FocusModeQSTile::class.java)
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

		doUpdateTile(FocusModeQSTile::class.java)
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
		val i = Intent(context, AppTimersBroadcastReceiver::class.java)
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
		val i = Intent(context, AppTimersBroadcastReceiver::class.java)
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
		if (reminderMin > 0 && timeLimitInternal.toMinutes() > reminderMin) {
			val u = ParsedUoid("Reminder", 0, toObserve).toString()
			if (!oidMap.contains(u)) {
				updatePrefs(u, makeOid())
			}
			setAppTimerInternal(u, toObserve, timeLimitInternal.minus(reminderMin.toLong(), ChronoUnit.MINUTES), null)
		}
		setAppTimerInternal(uoid, toObserve, timeLimitInternal, timeUsed)
	}
	// end time limit core

	fun onUpdatePowerConnection() {
		val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { context.registerReceiver(null, it) }

		val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
		val charging = chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
				chargePlug == BatteryManager.BATTERY_PLUGGED_AC

		doTrigger(!charging) { it is TimeChargerTriggerCondition && it.needCharger }
	}

	private fun ensureSchedSetup() {
		triggers.forEach { it.setup(context, this) }
	}

	private fun triggerFired(expire: Boolean, trigger: Trigger) {
		when (trigger.id) {
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
				BUG("invalid trigger id ${trigger.id} expire=$expire")
			}
		}
	}

	fun doTrigger(expire: Boolean, condition: (Trigger) -> Boolean) {
		triggers.forEach { fired ->
			if (condition(fired) && // is this the trigger we're searching for?
				(expire || // is this an deactivation request?
						(fired !is Condition) || // if this trigger is an condition, it needs to be fulfilled
						fired.isFulfilled(context, this))) {
				triggerFired(expire, fired)
			}
		}
	}

	fun onAlarmFired(id: String) {
		if ("alc" == id) {
			alc.fired()
			return
		}
		var t = false
		val nid = if (id.startsWith("expire::")) {
			t = true
			id.substring(8)
		} else id
		doTrigger(t) { it is TimeChargerTriggerCondition && it.iid == nid }
	}

	fun setTriggersForId(id: String, triggersIn: Array<out Trigger>) {
		triggers.filter { id == it.id }.forEach { it.dispose(context, this) }
		triggers = triggers.filterNot { id == it.id }.toSet().plus(triggersIn)
		writeSchedcfg()
		ensureSchedSetup()
	}

	fun getTriggersForId(id: String): List<Trigger> {
		return triggers.filter { id == it.id }
	}
}