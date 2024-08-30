package org.eu.droid_ng.wellbeing.framework

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import org.eu.droid_ng.wellbeing.shim.UserHandlerShim
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class WellbeingFrameworkServiceImpl(context: Context) :
	WellbeingFrameworkService.BaseWellbeingFrameworkService(context) {

	companion object {
		const val VERSION_CODE = 3
	}
	private val handler = Handler(Looper.getMainLooper())
	private var lastTime = 0L

	private val screenUnlockReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (intent?.action != Intent.ACTION_USER_PRESENT)
				return
			onScreenUnlock()
		}
	}

	override fun start() {
		try {
			context.registerReceiver(
				screenUnlockReceiver,
				IntentFilter(Intent.ACTION_USER_PRESENT),
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
					Context.RECEIVER_EXPORTED else 0
			)
		} catch (e: Exception) {
			// While errors are errors, if we crash, we cause the device to reboot.
			Log.e(TAG, Log.getStackTraceString(e))
		}
	}

	override fun stop() {
		try {
			context.unregisterReceiver(screenUnlockReceiver)
		} catch (e: Exception) {
			// While errors are errors, if we crash, we cause the device to reboot.
			Log.e(TAG, Log.getStackTraceString(e))
		}
	}

	private fun onScreenUnlock() {
		// Let's avoid reducing perf in CUJs by waiting three seconds
		handler.postDelayed({
			val midnight = LocalDateTime.now().withHour(0).withMinute(0)
				.withSecond(0).withNano(0).atZone(ZoneId.systemDefault())
				.toEpochSecond() * 1000
			val lastTimePlus12H = lastTime + 12 * 60 * 60 * 1000
			if (lastTimePlus12H <= System.currentTimeMillis() || midnight >= lastTime) {
				lastTime = System.currentTimeMillis()
				context.startForegroundService(
					Intent().setComponent(
						ComponentName(
							"org.eu.droid_ng.wellbeing",
							"org.eu.droid_ng.wellbeing.lib.EventProcessingService"
						)
					)
				)
			}
		}, 3000)
	}

	// since 1
	@Throws(RemoteException::class)
	override fun versionCode(): Int {
		return VERSION_CODE
	}

	// since 1
	@Throws(RemoteException::class)
	override fun setAirplaneMode(value: Boolean) {
		bgHandler.post {
			Settings.Global.putInt(
				context.contentResolver,
				Settings.Global.AIRPLANE_MODE_ON, if (value) 1 else 0
			)
			context.sendBroadcastAsUser(
				Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
					.putExtra("state", value), UserHandlerShim.ALL
			)
		}
	}

	// only in 2
	@Throws(RemoteException::class)
	override fun onNotificationPosted(packageName: String) {
		throw IllegalArgumentException("no longer supported")
	}

	// only in 2
	@Throws(RemoteException::class)
	override fun getEventCount(type: String, dimension: Int, from: Long, to: Long): Long {
		throw IllegalArgumentException("no longer supported")
	}

	// only in 2
	@Throws(RemoteException::class)
	override fun getTypesForPrefix(type: String, dimension: Int, from: Long, to: Long): Map<out Any?, Any?> {
		throw IllegalArgumentException("no longer supported")
	}
}