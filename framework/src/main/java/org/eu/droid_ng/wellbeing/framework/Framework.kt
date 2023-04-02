package org.eu.droid_ng.wellbeing.framework

import android.app.Application

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