package org.eu.droid_ng.wellbeing.lib

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import android.util.Log
import org.eu.droid_ng.wellbeing.broadcast.AlarmFiresBroadcastReciever
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ScheduleUtils {
	companion object {
		fun getPintentForId(context: Context, id: String): PendingIntent {
			return PendingIntent.getBroadcast(
				context, 0,
				Intent(context, AlarmFiresBroadcastReciever::class.java).addFlags(Intent.FLAG_RECEIVER_FOREGROUND).setIdentifier(id),
				PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
			)
		}

		fun dropAlarm(context: Context, id: String, alarmManager: AlarmManager? = null, pintent: PendingIntent? = null) {
			(alarmManager ?: context.getSystemService(AlarmManager::class.java))
				.cancel(pintent ?: getPintentForId(context, id))
		}

		fun setAlarm(context: Context, id: String, time: LocalDateTime, alarmManager: AlarmManager? = null, pintent: PendingIntent? = null) {
			val am = alarmManager ?: context.getSystemService(AlarmManager::class.java)
			val pi = pintent ?: getPintentForId(context, id)
			dropAlarm(context, id, am, pi)
			am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
				time.withSecond(0).atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L, pi)
		}
	}
}

interface Trigger {
	val id: String
	fun setup(applicationContext: Context, service: WellbeingService)
	fun dispose(applicationContext: Context, service: WellbeingService)
}

interface Condition {
	val id: String
	fun isFulfilled(applicationContext: Context, service: WellbeingService): Boolean
}

interface TriggerCondition : Trigger, Condition

class TimeTriggerCondition(override val id: String, val startHour: Int, val startMinute: Int, val endHour: Int, val endMinute: Int) : TriggerCondition {
	override fun setup(applicationContext: Context, service: WellbeingService) {
		val now = LocalDateTime.now().withNano(0)
		val start = now.withSecond(0).withHour(startHour).withMinute(startMinute).let {
			if (now.isEqual(it) || now.isAfter(it)) {
				it.plusDays(1)
			} else {
				it
			}
		}
		val end = now.withSecond(0).withHour(endHour).withMinute(endMinute).let {
			if (now.isEqual(it) || now.isAfter(it)) {
				it.plusDays(1)
			} else {
				it
			}
		}
		ScheduleUtils.setAlarm(applicationContext, id, start)
		ScheduleUtils.setAlarm(applicationContext, "expire::$id", end)
	}

	override fun dispose(applicationContext: Context, service: WellbeingService) {
		ScheduleUtils.dropAlarm(applicationContext, id)
		ScheduleUtils.dropAlarm(applicationContext, "expire::$id")
	}

	override fun isFulfilled(applicationContext: Context, service: WellbeingService): Boolean {
		val now = LocalDateTime.now().withNano(0)
		val end = now.withSecond(0).withHour(endHour).withMinute(endMinute)
		val start = now.withSecond(0).withHour(startHour).withMinute(startMinute).let {
			if (it.isAfter(end)) {
				it.minusDays(1)
			} else {
				it
			}
		}
		return ((now.isAfter(start) || now.isEqual(start)) && now.isBefore(end))
	}
}

class ChargerTriggerCondition(override val id: String): TriggerCondition {
	override fun setup(applicationContext: Context, service: WellbeingService) {
		/* No setup needed, PowerConnectionBroadcastReciever is all we need */
	}

	override fun dispose(applicationContext: Context, service: WellbeingService) {
		/* No disposal needed as we don't setup anything */
	}

	override fun isFulfilled(applicationContext: Context, service: WellbeingService): Boolean {
		val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { applicationContext.registerReceiver(null, it) }

		val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
		return chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
				chargePlug == BatteryManager.BATTERY_PLUGGED_AC
	}
}