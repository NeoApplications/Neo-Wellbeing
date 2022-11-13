package org.eu.droid_ng.wellbeing.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.eu.droid_ng.wellbeing.lib.WellbeingService

class NotificationBroadcastReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		// Looks weird, but we don't want to crash if someone feeds us junk
		intent.action?.let { WellbeingService.get().onNotificationActionClick(it) }
	}
}