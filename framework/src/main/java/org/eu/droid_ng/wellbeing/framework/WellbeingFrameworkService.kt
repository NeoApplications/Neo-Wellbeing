package org.eu.droid_ng.wellbeing.framework

import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder

class WellbeingFrameworkService : Service() {
	private var wellbeingFrameworkService: BaseWellbeingFrameworkService? = null

	override fun onBind(intent: Intent): IBinder? {
		return if ("org.eu.droid_ng.wellbeing.framework.FRAMEWORK_SERVICE" == intent.action && wellbeingFrameworkService != null)
			wellbeingFrameworkService
		else
			null
	}

	private fun createFwkService(): BaseWellbeingFrameworkService {
		if (Framework.hasService()) return Framework.getService()
		return WellbeingFrameworkServiceImpl(this)
	}

	override fun onCreate() {
		super.onCreate()
		if (wellbeingFrameworkService == null)
			wellbeingFrameworkService = createFwkService()
		wellbeingFrameworkService!!.doStart()
	}

	override fun onDestroy() {
		wellbeingFrameworkService?.doStop()
		super.onDestroy()
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		intent?.let {
			wellbeingFrameworkService!!.onStartCommand(it)
		}
		return START_STICKY
	}

	abstract class BaseWellbeingFrameworkService(protected val context: Context) :
		IWellbeingFrameworkService.Stub() {
		companion object {
			@JvmStatic protected val TAG = "WellbeingFrameworkService"
		}

		protected val uiHandler = Handler(context.mainLooper)
		protected lateinit var bgHandler: Handler
		private lateinit var bgThread: HandlerThread

		fun doStart() {
			bgThread = HandlerThread(TAG)
			bgThread.start()
			bgHandler = Handler(bgThread.looper)
			start()
		}

		fun doStop() {
			stop()
			bgThread.quitSafely()
			bgThread.join()
		}

		protected fun isInWorkProfile(): Boolean {
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

		protected abstract fun start()
		abstract fun onStartCommand(intent: Intent)
		protected abstract fun stop()
	}
}