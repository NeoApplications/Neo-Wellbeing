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
import org.eu.droid_ng.wellbeing.shim.UserHandlerShim

class WellbeingFrameworkServiceImpl(context: Context) :
	WellbeingFrameworkService.BaseWellbeingFrameworkService(context) {

	private val screenUnlockReceiver = object : BroadcastReceiver() {
		override fun onReceive(context: Context?, intent: Intent?) {
			if (intent?.action != Intent.ACTION_USER_PRESENT)
				return
			Log.i(TAG, intent.toString())
			doCountScreenUnlock()
		}
	}

	override fun start() {
		bgHandler.post {
			Log.i("Wellbeing", "starting in " + android.os.Process.myPid())
			Framework.setService(this)
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
			context.registerReceiver(screenUnlockReceiver, IntentFilter(Intent.ACTION_USER_PRESENT))
		}
	}

	override fun onStartCommand(intent: Intent) {
		if (intent.getBooleanExtra("addOneUnlock", false))
			doCountScreenUnlock()
	}

	override fun stop() {
		bgHandler.post {
			context.unregisterReceiver(screenUnlockReceiver)
			Framework.setService(null)
		}
	}

	private fun doCountScreenUnlock() {
		// TODO
		Log.i(TAG, "counted screen unlock")
	}

	fun onNotificationPosted(packageName: String) {
		// TODO
		Log.i(TAG, "counted notification by $packageName")
	}

	@Throws(RemoteException::class)
	override fun versionCode(): Int {
		return 1
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
}