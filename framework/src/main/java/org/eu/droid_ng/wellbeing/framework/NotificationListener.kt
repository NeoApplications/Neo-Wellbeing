package org.eu.droid_ng.wellbeing.framework

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.eu.droid_ng.wellbeing.shared.WellbeingFrameworkClient

class NotificationListener : NotificationListenerService(), WellbeingFrameworkClient.ConnectionCallback {
	private var service: WellbeingFrameworkClient? = null
	private val seenNotifications = HashSet<Notification>()
	private var missedNotifications: ArrayList<Notification>? = ArrayList()

	override fun onCreate() {
		super.onCreate()
		service = WellbeingFrameworkClient(this, this)
	}

	override fun onDestroy() {
		super.onDestroy()
		service?.tryDisconnect()
		service = null
	}

	override fun onListenerConnected() {
		super.onListenerConnected()
		service?.tryConnect()
	}

	override fun onListenerDisconnected() {
		super.onListenerDisconnected()
		seenNotifications.clear()
		service?.tryDisconnect()
		service = null
	}

	override fun onNotificationPosted(sbn: StatusBarNotification?) {
		if (sbn == null) return
		val n = Notification(sbn.id, sbn.tag, sbn.packageName)
		if (seenNotifications.add(n)) {
			// Either missedNotifications or service is always non-null
			if (missedNotifications != null) {
				missedNotifications?.add(n)
			} else if (service != null) {
				service?.onNotificationPosted(sbn.packageName)
			} else {
				// TODO BUG()
			}
		}
	}

	override fun onNotificationRemoved(
		sbn: StatusBarNotification?,
		rankingMap: RankingMap?,
		reason: Int
	) {
		if (sbn == null) return
		seenNotifications.remove(Notification(sbn.id, sbn.tag, sbn.packageName))
	}

	override fun onWellbeingFrameworkConnected(initial: Boolean) {
		missedNotifications?.forEach { service?.onNotificationPosted(it.packageName) }
		missedNotifications = null
	}

	override fun onWellbeingFrameworkDisconnected() {
		missedNotifications = ArrayList()
	}

	private data class Notification(val id: Int, val tag: String?, val packageName: String)
}