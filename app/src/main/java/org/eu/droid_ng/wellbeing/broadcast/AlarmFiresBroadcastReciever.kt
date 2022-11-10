package org.eu.droid_ng.wellbeing.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.eu.droid_ng.wellbeing.lib.WellbeingService

class AlarmFiresBroadcastReciever : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
		intent?.identifier?.let { WellbeingService.get().onAlarmFired(it) }
	}
}