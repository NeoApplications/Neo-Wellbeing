package org.eu.droid_ng.wellbeing.framework

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.RemoteException
import android.provider.Settings
import android.util.Log
import org.eu.droid_ng.wellbeing.shared.Database
import org.eu.droid_ng.wellbeing.shared.TimeDimension
import org.eu.droid_ng.wellbeing.shim.UserHandlerShim
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId


class WellbeingFrameworkServiceImpl(context: Context) :
	WellbeingFrameworkService.BaseWellbeingFrameworkService(context) {

	private var tracksScreenUnlock = false
	private lateinit var db: Database

	private val screenUnlockReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (intent?.action != Intent.ACTION_USER_PRESENT)
				return
			onScreenUnlock()
		}
	}

	override fun start() {
		bgHandler.post {
			Framework.setService(this)
			db = Database(context, bgHandler)
			tracksScreenUnlock = !isInWorkProfile()
			// Ensure we have notification permission
			val nm = context.getSystemService(NotificationManager::class.java)
			if (!nm.isNotificationPolicyAccessGranted) {
				// Grant ourselves notification permission (the perks of being a system app :D)
				val cn = ComponentName(context, NotificationListener::class.java).flattenToString()
				val value = Settings.Secure.getString(
					context.contentResolver,
					"enabled_notification_listeners"
				)
				Settings.Secure.putString(
					context.contentResolver,
					"enabled_notification_listeners",
					if (!value.isNullOrEmpty()) "${value}:${cn}" else cn
				)
				Thread.sleep(500)
				if (!nm.isNotificationPolicyAccessGranted) {
					// Eh?
					Log.e(TAG, "failed to grant myself notification access")
				}
			}
			if (tracksScreenUnlock) {
				try {
					context.registerReceiver(
						screenUnlockReceiver,
						IntentFilter(Intent.ACTION_USER_PRESENT)
					)
				} catch (e: Exception) {
					// While errors are errors, if we crash, we cause the device to reboot.
					Log.e(TAG, Log.getStackTraceString(e))
				}
			}
		}
	}

	override fun onStartCommand(intent: Intent) {
		if (intent.getBooleanExtra("addOneUnlock", false))
			onScreenUnlock()
	}

	override fun stop() {
		bgHandler.post {
			if (tracksScreenUnlock) {
				try {
					context.unregisterReceiver(screenUnlockReceiver)
				} catch (e: Exception) {
					// While errors are errors, if we crash, we cause the device to reboot.
					Log.e(TAG, Log.getStackTraceString(e))
				}
			}
			Framework.setService(null)
		}
	}

	private fun doCountScreenUnlock() {
		// TODO
		Log.i(TAG, "counted screen unlock")
		db.incrementNow("unlock")
	}

	private fun doCountNotificationPosted(packageName: String) {
		// TODO
		Log.i(TAG, "counted notification by $packageName")
		db.incrementNow("notif_$packageName")
	}

	private fun doGetEventCount(type: String, from: LocalDateTime, to: LocalDateTime, dimension: TimeDimension): Long {
		return db.getCountFor(type, dimension, from, to)
	}

	fun onScreenUnlock() {
		if (tracksScreenUnlock) {
			bgHandler.post {
				doCountScreenUnlock()
			}
		}
	}

	// since 1
	@Throws(RemoteException::class)
	override fun versionCode(): Int {
		return 2
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

	// since 2
	@Throws(RemoteException::class)
	override fun onNotificationPosted(packageName: String) {
		bgHandler.post {
			doCountNotificationPosted(packageName)
		}
	}

	// since 2
	@Throws(RemoteException::class)
	override fun getEventCount(type: String, from: Long, to: Long, dimension: Int): Long {
		Log.i("wellbeingh", "getEventCount()")
		return doGetEventCount(type,
			LocalDateTime.ofInstant(Instant.ofEpochSecond(from), ZoneId.systemDefault()),
			LocalDateTime.ofInstant(Instant.ofEpochSecond(to), ZoneId.systemDefault()),
			TimeDimension.values()[dimension]
		)
	}
}