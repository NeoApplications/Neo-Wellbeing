package org.eu.droid_ng.wellbeing.framework

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder

class WellbeingFrameworkService : Service() {
	override fun onBind(intent: Intent): IBinder? {
		return if ("org.eu.droid_ng.wellbeing.framework.FRAMEWORK_SERVICE" == intent.action)
			Framework.getService()
		else
			null
	}

	override fun onCreate() {
		super.onCreate()
		if (Framework.getService() == null)
			Framework.setService(WellbeingFrameworkServiceImpl(this))
		val ws = Framework.getService()!!
		ws.bgThread.start()
		ws.start()
	}

	override fun onDestroy() {
		val ws = Framework.getService()
		ws?.stop()
		ws?.bgThread?.quitSafely()
		ws?.bgThread?.join()
		super.onDestroy()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		return START_STICKY
	}

	abstract class BaseWellbeingFrameworkService(protected val context: Context) :
		IWellbeingFrameworkService.Stub() {
		companion object {
			@JvmStatic protected val TAG = "WellbeingFrameworkService"
		}

		internal val bgThread = HandlerThread(TAG)
		protected val bgHandler by lazy { Handler(bgThread.looper
			?: throw IllegalStateException("used bgHandler before start() was called")) }

		abstract fun start()
		abstract fun stop()
	}
}