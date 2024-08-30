package org.eu.droid_ng.wellbeing.framework

import android.content.Context
import android.content.Intent
import android.os.RemoteException
import android.provider.Settings
import org.eu.droid_ng.wellbeing.shim.UserHandlerShim

class WellbeingFrameworkServiceImpl(context: Context) :
	WellbeingFrameworkService.BaseWellbeingFrameworkService(context) {

	companion object {
		const val VERSION_CODE = 3
	}

	override fun start() {
		// do nothing
	}

	override fun stop() {
		// do nothing
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