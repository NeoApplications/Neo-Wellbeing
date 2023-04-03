package org.eu.droid_ng.wellbeing.framework

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log

class WellbeingBootReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
		if (intent?.action != "android.intent.action.BOOT_COMPLETED")
			return
		if (Framework.hasService()) {
			Framework.getService().onScreenUnlock() // The first unlock after boot
		} else {
			context?.let { it.startService(Intent(it, WellbeingFrameworkService::class.java).putExtra("addOneUnlock", true)) }
		}
	}
}