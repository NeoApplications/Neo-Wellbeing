package org.eu.droid_ng.wellbeing.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.Utils.clearUsageStatsCache
import org.eu.droid_ng.wellbeing.lib.Utils.getMostUsedPackages
import org.eu.droid_ng.wellbeing.lib.Utils.getScreenTime
import org.eu.droid_ng.wellbeing.lib.Utils.getTimeUsed
import org.eu.droid_ng.wellbeing.lib.WellbeingService.Companion.get
import java.time.Duration

class ScreenTimeAppWidget : AppWidgetProvider() {
	private var pendingIntent: PendingIntent? = null

	override fun onReceive(context: Context, intent: Intent) {
		super.onReceive(context, intent)

		if ("org.eu.droid_ng.wellbeing.APPWIDGET_UPDATE" == intent.action) {
			val awm = AppWidgetManager.getInstance(context)
			onUpdate(
				context,
				awm,
				awm.getAppWidgetIds(ComponentName(context, ScreenTimeAppWidget::class.java))
			)
		}
	}

	private fun checkInitialize(context: Context) {
		if (pendingIntent == null) {
			val intent = Intent("com.android.settings.action.IA_SETTINGS")
			intent.setPackage(context.packageName)
			pendingIntent = PendingIntent.getActivity(
				context, this.hashCode(),
				intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
			)
		}
	}

	override fun onEnabled(context: Context) {
		checkInitialize(context)
	}

	override fun onUpdate(
		context: Context,
		appWidgetManager: AppWidgetManager,
		appWidgetIds: IntArray
	) {
		checkInitialize(context)
		clearUsageStatsCache(get().usm, context.packageManager, get().pmd, true)

		for (appWidgetId in appWidgetIds) {
			appWidgetManager.updateAppWidget(
				appWidgetId,
				updateLayout(context)
			)
		}
	}

	override fun onDeleted(context: Context, appWidgetIds: IntArray) {
	}

	override fun onDisabled(context: Context) {
	}

	private fun updateLayout(
		context: Context
	): RemoteViews {
		val usm = get().usm
		val remoteViews = RemoteViews(
			context.packageName, R.layout.appwidget_screen_time
		)

		remoteViews.setOnClickPendingIntent(R.id.appwidget_root, pendingIntent)
		remoteViews.setTextViewText(
			R.id.appwidget_screen_time,
			formatDuration(getScreenTime(usm))
		)
		val mostUsedPackages = getMostUsedPackages(usm)
		for (i in appViewIds.indices) {
			if (i >= mostUsedPackages.size) {
				remoteViews.setViewVisibility(appView3Ids[i], View.GONE)
				remoteViews.setViewVisibility(appViewIds[i], View.GONE)
			} else {
				remoteViews.setViewVisibility(appViewIds[i], View.VISIBLE)
				remoteViews.setViewVisibility(appView3Ids[i], View.VISIBLE)
				val packageName = mostUsedPackages[i]
				var packageLabel = packageName
				try {
					packageLabel = get()
						.getApplicationLabel(packageName).toString()
				} catch (e: PackageManager.NameNotFoundException) {
					Log.e("ScreenTimeAppWidget", "Failed to get app label!")
				}

				remoteViews.setTextViewText(appViewIds[i], packageLabel)
				remoteViews.setTextViewText(
					appView2Ids[i],
					formatDuration(getTimeUsed(usm, packageName))
				)
			}
		}
		return remoteViews
	}

	companion object {
		private val appViewIds = intArrayOf(
			R.id.appwidget_app1_n, R.id.appwidget_app2_n, R.id.appwidget_app3_n
		)
		private val appView2Ids = intArrayOf(
			R.id.appwidget_app1_t, R.id.appwidget_app2_t, R.id.appwidget_app3_t
		)
		private val appView3Ids = intArrayOf(
			R.id.appwidget_app1_l, R.id.appwidget_app2_l, R.id.appwidget_app3_l
		)

		private fun formatDuration(duration: Duration): String {
			val hours = duration.toHours()
			var minutes = duration.toMinutes()
			minutes -= (hours * 60)
			return if (hours == 0L) {
				minutes.toString() + "m"
			} else if (minutes == 0L) {
				hours.toString() + "h"
			} else {
				hours.toString() + "h " +
						minutes + "m"
			}
		}
	}
}