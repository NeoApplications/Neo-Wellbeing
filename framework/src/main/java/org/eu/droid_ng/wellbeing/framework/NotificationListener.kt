package org.eu.droid_ng.wellbeing.framework

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.eu.droid_ng.wellbeing.shared.WellbeingFrameworkClient

class NotificationListener : NotificationListenerService(), WellbeingFrameworkClient.ConnectionCallback {
	private var service: WellbeingFrameworkClient? = null
	private val seenNotifications = ArrayList<Pair<String, Int>>()
	private var missedNotifications: ArrayList<String>? = ArrayList()

	override fun onCreate() {
		super.onCreate()
		service = WellbeingFrameworkClient(this, this)
	}

	override fun onDestroy() {
		super.onDestroy()
		service?.tryDisconnect()
	}

	override fun onListenerConnected() {
		super.onListenerConnected()
		service?.tryConnect()
	}

	override fun onListenerDisconnected() {
		super.onListenerDisconnected()
		seenNotifications.clear()
		service?.tryDisconnect()
	}

	override fun onNotificationPosted(sbn: StatusBarNotification?) {
		super.onNotificationPosted(sbn)
		sbn?.let { n ->
			if (seenNotifications.find { it.first == n.packageName && it.second == n.id } == null) {
				seenNotifications.add(Pair(n.packageName, n.id))
				missedNotifications?.add(n.packageName)
				service?.onNotificationPosted(n.packageName)
			}
		}
	}

	override fun onWellbeingFrameworkConnected(initial: Boolean) {
		missedNotifications?.forEach { service?.onNotificationPosted(it) }
		missedNotifications = null
	}

	override fun onWellbeingFrameworkDisconnected() {
		missedNotifications = ArrayList()
	}
}