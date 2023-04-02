package org.eu.droid_ng.wellbeing.framework

import android.os.Handler
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationListener : NotificationListenerService() {
	private var service: WellbeingFrameworkServiceImpl? = null
	private lateinit var handler: Handler
	private val seenNotifications = ArrayList<Pair<String, Int>>()
	private var missedNotifications: ArrayList<String>? = ArrayList()

	override fun onCreate() {
		super.onCreate()
		handler = Handler(mainLooper)
	}

	override fun onListenerConnected() {
		super.onListenerConnected()
		connectToService()
	}

	fun connectToService() {
		if (service != null)
			return
		if (Framework.hasService()) {
			service = Framework.getService()
			missedNotifications?.forEach { service?.onNotificationPosted(it) }
			missedNotifications = null
		} else {
			handler.postDelayed({ connectToService() }, 500)
		}
	}

	override fun onListenerDisconnected() {
		super.onListenerDisconnected()
		seenNotifications.clear()
		missedNotifications = ArrayList()
		service = null
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
}