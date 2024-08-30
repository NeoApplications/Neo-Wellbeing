package org.eu.droid_ng.wellbeing.framework

import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WellbeingBootReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context, intent: Intent?) {
		if (intent?.action != "android.intent.action.BOOT_COMPLETED")
			return
		if (!isInWorkProfile(context))
			context.startService(Intent(context, WellbeingFrameworkService::class.java))
	}

	private fun isInWorkProfile(context: Context): Boolean {
		val devicePolicyManager =
			context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
		val activeAdmins = devicePolicyManager.activeAdmins
		if (activeAdmins != null) {
			for (admin in activeAdmins) {
				if (devicePolicyManager.isProfileOwnerApp(admin.packageName))
					return true
			}
		}
		return false
	}
}