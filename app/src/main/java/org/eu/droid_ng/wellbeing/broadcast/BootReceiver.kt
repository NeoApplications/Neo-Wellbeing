package org.eu.droid_ng.wellbeing.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.eu.droid_ng.wellbeing.lib.WellbeingService

class BootReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if ("android.intent.action.BOOT_COMPLETED" != intent.action) {
			/* Make sure no one is trying to fool us */
			return
		}
		WellbeingService.get().onBootCompleted()
	}
}