package org.eu.droid_ng.wellbeing.framework

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WellbeingBootReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
		if (intent?.action != "android.intent.action.BOOT_COMPLETED")
			return
		if (!Framework.hasService()) {
			context?.let { it.startService(Intent(it, WellbeingFrameworkService::class.java).putExtra("addOneUnlock", true)) }
		}
	}
}