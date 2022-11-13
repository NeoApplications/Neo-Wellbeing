package org.eu.droid_ng.wellbeing.lib

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import org.eu.droid_ng.wellbeing.broadcast.AlarmFiresBroadcastReciever
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class ScheduleUtils {
	companion object {
		private fun getPintentForId(context: Context, id: String): PendingIntent {
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
	val iid: String
	fun setup(applicationContext: Context, service: WellbeingService)
	fun dispose(applicationContext: Context, service: WellbeingService)
}

interface Condition {
	val id: String
	fun isFulfilled(applicationContext: Context, service: WellbeingService): Boolean
}

class TimeChargerTriggerCondition(
	override val id: String,
	override val iid: String,
	val startHour: Int,
	val startMinute: Int,
	val endHour: Int,
	val endMinute: Int,
	val weekdays: BooleanArray, // length = 7, 0 = monday, 6 = sunday
	val needCharger: Boolean
) : Trigger, Condition {
	override fun setup(applicationContext: Context, service: WellbeingService) {
		if (!weekdays.any { it }) return // bail if no weekday is enabled
		val now = LocalDateTime.now().withNano(0)
		val cwd = if (!weekdays[now.dayOfWeek.ordinal]) {
			val offset = now.dayOfWeek.ordinal
			var r = now
			for (i in 0..6) {
				val j = (i + offset) % 7
				if (weekdays[j]) {
					r = now.with(TemporalAdjusters.next(DayOfWeek.of(j + 1)))
					break
				}
			}
			if (r == now) {
				throw IllegalStateException("this cannot happen, r == now")
			}
			r
		} else now
		val offset = cwd.dayOfWeek.ordinal
		var nwd = cwd
		for (i in 1..7) {
			val j = (i + offset) % 7
			if (weekdays[j]) {
				nwd = cwd.with(TemporalAdjusters.next(DayOfWeek.of(j + 1)))
				break
			}
		}
		if (nwd == cwd) {
			throw IllegalStateException("this cannot happen, nwd == cwd")
		}
		val start = cwd.withSecond(0).withHour(startHour).withMinute(startMinute).let {
			if (now.isEqual(it) || now.isAfter(it)) {
				nwd.withSecond(0).withHour(startHour).withMinute(startMinute)
			} else {
				it
			}
		}
		nwd = start
		for (i in 1..7) {
			val j = (i + offset) % 7
			if (weekdays[j]) {
				nwd = start.with(TemporalAdjusters.next(DayOfWeek.of(j + 1)))
				break
			}
		}
		if (nwd == start) {
			throw IllegalStateException("this cannot happen, nwd == start")
		}
		val end = cwd.withSecond(0).withHour(endHour).withMinute(endMinute).let {
			if (now.isEqual(it) || now.isAfter(it)) {
				nwd.withSecond(0).withHour(endHour).withMinute(endMinute)
			} else {
				it
			}
		}
		ScheduleUtils.setAlarm(applicationContext, iid, start)
		ScheduleUtils.setAlarm(applicationContext, "expire::$iid", end)
	}

	override fun dispose(applicationContext: Context, service: WellbeingService) {
		ScheduleUtils.dropAlarm(applicationContext, iid)
		ScheduleUtils.dropAlarm(applicationContext, "expire::$iid")
	}

	override fun isFulfilled(applicationContext: Context, service: WellbeingService): Boolean {
		val now = LocalDateTime.now().withNano(0)
		return (weekdays[now.dayOfWeek.ordinal] && run {
			val end = now.withSecond(0).withHour(endHour).withMinute(endMinute)
			val start = now.withSecond(0).withHour(startHour).withMinute(startMinute).let {
				if (it.isAfter(end)) {
					it.minusDays(1)
				} else {
					it
				}
			}
			(now.isAfter(start) || now.isEqual(start)) && now.isBefore(end)
		}) && (!needCharger || run {
			val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { applicationContext.registerReceiver(null, it) }

			val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
			chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
					chargePlug == BatteryManager.BATTERY_PLUGGED_AC
		})
	}
}