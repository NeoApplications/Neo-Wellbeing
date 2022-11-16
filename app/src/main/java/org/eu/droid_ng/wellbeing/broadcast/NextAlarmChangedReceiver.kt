package org.eu.droid_ng.wellbeing.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.eu.droid_ng.wellbeing.lib.AlarmCoordinator

class NextAlarmChangedReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent?) {
		if ("android.app.action.NEXT_ALARM_CLOCK_CHANGED" != intent?.action) {
			/* Make sure no one is trying to fool us */
			return
		}
		AlarmCoordinator(context).updateState()
	}
}