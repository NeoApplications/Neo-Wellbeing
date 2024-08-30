package org.eu.droid_ng.wellbeing.lib

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.BugUtils.Companion.BUG

class EventProcessingService : Service() {
	companion object {
		private const val NOTIFICATION_ID = 1
		private const val CHANNEL_ID = "event_proc"
	}

	private val handler = Handler(Looper.getMainLooper())
	private var isBusy = false

	override fun onCreate() {
		val notificationManager = getSystemService(NotificationManager::class.java)
		val channel = NotificationChannel(CHANNEL_ID, getString(R.string.event_channel_name),
			NotificationManager.IMPORTANCE_LOW)
		channel.description = getString(R.string.event_channel_description)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			channel.isBlockable = true
		}
		notificationManager.createNotificationChannel(channel)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		startForeground(
			NOTIFICATION_ID, Notification.Builder(this, CHANNEL_ID)
				.setContentTitle(getString(R.string.event_notif_name))
				.setContentText(getString(R.string.event_notif_desc))
				.setSmallIcon(R.drawable.ic_stat_name)
				.build()
		)
		handler.postDelayed({
			if (!isBusy) {
				BUG("leaked event processing service?")
				stopSelf()
			}
		}, 5 * 60 * 1000)
		if (!isBusy)
			Thread {
				try {
					isBusy = true
					processEvents()
				} finally {
					isBusy = false
					stopSelf()
				}
			}.start()
		return START_NOT_STICKY
	}

	override fun onBind(intent: Intent?): IBinder? {
		return null
	}

	override fun onDestroy() {
		if (isBusy) {
			BUG("reverse leaked event processing service????")
		}
		handler.removeCallbacksAndMessages(null)
	}

	private fun processEvents() {
		Thread.sleep(20000)
	}
}