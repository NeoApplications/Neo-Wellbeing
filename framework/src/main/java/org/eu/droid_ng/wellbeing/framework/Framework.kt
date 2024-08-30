package org.eu.droid_ng.wellbeing.framework

import android.app.Application
import android.util.Log
import org.eu.droid_ng.wellbeing.shared.BugUtils

class Framework : Application() {
	companion object {
		private const val TAG = "WellbeingFramework"
		private lateinit var application: Framework

		fun setService(service: WellbeingFrameworkServiceImpl?) {
			return application.setServiceInternal(service)
		}

		fun getService(): WellbeingFrameworkServiceImpl? {
			return application.getServiceInternal()
		}
	}
	private var service: WellbeingFrameworkServiceImpl? = null

	init {
		// While it's... quite bad if we get uncaught exceptions, it's even worse if we crash.
		// If Android can't keep us alive, the device gets thrown into a boot loop.
		// This is also why this app should be kept as simple as possible.
		Thread.setDefaultUncaughtExceptionHandler { _, e ->
			Log.e(TAG, Log.getStackTraceString(e))
			BugUtils.get()?.onBugAdded(e, System.currentTimeMillis())
		}
	}

	override fun onCreate() {
		super.onCreate()
		application = this
		BugUtils.maybeInit(this)
		Thread {
			// maybe it'll be useful in the future
			val prefs = getSharedPreferences("framework", 0)
			if (prefs.getInt("version", 0) != WellbeingFrameworkServiceImpl.VERSION_CODE) {
				prefs.edit().putInt("version", WellbeingFrameworkServiceImpl.VERSION_CODE).apply()
			}
			// == temp migration code start ==
			try {
				filesDir.listFiles()?.forEach { it.deleteRecursively() }
			} catch (e: Exception) {
				Log.e(TAG, Log.getStackTraceString(e))
			}
			// == temp migration code end ==
		}.start()
	}

	private fun getServiceInternal(): WellbeingFrameworkServiceImpl? {
		return service
	}

	private fun setServiceInternal(service: WellbeingFrameworkServiceImpl?) {
		this.service = service
	}
}