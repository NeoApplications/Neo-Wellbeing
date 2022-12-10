package org.eu.droid_ng.wellbeing.framework

import android.content.Context
import android.content.Intent
import android.os.RemoteException
import android.provider.Settings
import org.eu.droid_ng.wellbeing.shim.UserHandlerShim

class WellbeingFrameworkServiceImpl(private val context: Context) :
	IWellbeingFrameworkService.Stub() {
	@Throws(RemoteException::class)
	override fun versionCode(): Int {
		return 1
	}

	@Throws(RemoteException::class)
	override fun setAirplaneMode(value: Boolean) {
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