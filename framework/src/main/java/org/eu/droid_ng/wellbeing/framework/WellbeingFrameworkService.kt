package org.eu.droid_ng.wellbeing.framework

import android.app.Service
import android.content.Intent
import android.os.IBinder

class WellbeingFrameworkService : Service() {
	private val wellbeingFrameworkService = WellbeingFrameworkServiceImpl(this)
	override fun onBind(intent: Intent): IBinder? {
		return if ("org.eu.droid_ng.wellbeing.framework.FRAMEWORK_SERVICE" != intent.action) null else wellbeingFrameworkService
	}
}