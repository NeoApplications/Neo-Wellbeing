package org.eu.droid_ng.wellbeing

import android.app.Application
import org.eu.droid_ng.wellbeing.lib.BugUtils
import kotlin.system.exitProcess

class Wellbeing : Application() {
	override fun onCreate() {
		super.onCreate()
		BugUtils.maybeInit(this)
		Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable ->
			BugUtils.get()?.onBugAdded(paramThrowable, System.currentTimeMillis())
			exitProcess(2)
		}
	}
}