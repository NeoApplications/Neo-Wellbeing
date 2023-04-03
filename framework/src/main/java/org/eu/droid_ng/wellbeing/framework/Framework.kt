package org.eu.droid_ng.wellbeing.framework

import android.app.Application
import android.util.Log

class Framework : Application() {
	companion object {
		private lateinit var application: Framework

		@JvmStatic
		fun setService(service: WellbeingFrameworkServiceImpl?) {
			return application.setServiceInternal(service)
		}

		@JvmStatic
		fun getService(): WellbeingFrameworkServiceImpl {
			return application.getServiceInternal()!!
		}

		@JvmStatic
		fun hasService(): Boolean {
			return application.getServiceInternal() != null
		}
	}
	private var service: WellbeingFrameworkServiceImpl? = null

	init {
		// While it's... quite bad if we get uncaught exceptions, it's even worse if we crash. If Android can't keep us alive, the device gets thrown into a bootloop
		Thread.setDefaultUncaughtExceptionHandler { _, e -> Log.e("WellbeingFramework", Log.getStackTraceString(e)) }
	}

	override fun onCreate() {
		super.onCreate()
		application = this
	}

	private fun getServiceInternal(): WellbeingFrameworkServiceImpl? {
		return service
	}

	private fun setServiceInternal(service: WellbeingFrameworkServiceImpl?) {
		this.service = service
	}
}