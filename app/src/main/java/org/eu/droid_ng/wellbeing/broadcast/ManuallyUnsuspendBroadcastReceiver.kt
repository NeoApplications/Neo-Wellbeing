package org.eu.droid_ng.wellbeing.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.eu.droid_ng.wellbeing.shared.BugUtils.Companion.BUG
import org.eu.droid_ng.wellbeing.lib.WellbeingService

class ManuallyUnsuspendBroadcastReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent) {
		if ("android.intent.action.PACKAGE_UNSUSPENDED_MANUALLY" != intent.action) {
			/* Make sure no one is trying to fool us */
			return
		}
		val packageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)
		if (packageName == null) {
			/* Make sure we have a package name */
			Toast.makeText(
				context,
				"Assertion failure (0xAC): packageName is null. Please report this to the developers!",
				Toast.LENGTH_LONG
			).show()
			BUG("packageName == null (0xAC)")
			return
		}
		WellbeingService.get().onManuallyUnsuspended(packageName)
	}
}