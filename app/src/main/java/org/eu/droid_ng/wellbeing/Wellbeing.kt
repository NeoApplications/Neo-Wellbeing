package org.eu.droid_ng.wellbeing

import android.app.Application
import org.eu.droid_ng.wellbeing.lib.BugUtils
import org.eu.droid_ng.wellbeing.lib.WellbeingService
import kotlin.system.exitProcess

class Wellbeing : Application() {
	companion object {
		private lateinit var application: Wellbeing

		fun getService(): WellbeingService {
			return application.getServiceInternal()
		}
	}

	private lateinit var service: WellbeingService

	override fun onCreate() {
		super.onCreate()
		application = this
		BugUtils.maybeInit(this)
		Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable ->
			BugUtils.get()?.onBugAdded(paramThrowable, System.currentTimeMillis())
			exitProcess(2)
		}

		service = WellbeingService(this)
	}

	private fun getServiceInternal(): WellbeingService {
		return service
	}
}