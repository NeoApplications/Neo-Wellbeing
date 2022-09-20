package org.eu.droid_ng.wellbeing.lib

import android.content.Context

class TransistentWellbeingState(val context: Context) {

	fun onManuallyUnsuspended(packageName: String) {
		val client = WellbeingStateClient(context)
		if (client.isServiceRunning()) client.doBindService({ state ->
			state!!.onManuallyUnsuspended(
				packageName
			)
		}, true) else AppTimersInternal.get(context).appTimerSuspendHook(packageName)
	}

	fun onAppTimerExpired(oid: Int, uoid: String) {
		AppTimersInternal.get(context).onBroadcastRecieve(oid, uoid)
	}

	fun onBootCompleted() {

	}
}