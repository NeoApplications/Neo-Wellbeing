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

		private fun triggerConditionFromString(raw: String, delim: String = ";;"): TriggerCondition? {
			val values = raw.split(delim)
			if (values.size < 2) throw IllegalStateException("invalid value $raw")
			return when (values[0]) {
				"charger" -> ChargerTriggerCondition(values[1])
				"time" -> {
					val bools = BooleanArray(7); for (i in bools.indices) if (values[6].toInt() and (1 shl i) != 0) bools[i] = true // bitmask -> boolean[]
					TimeTriggerCondition(values[1], values[2].toInt(), values[3].toInt(), values[4].toInt(), values[5].toInt(), bools)
				}
				"tcdual" ->
					TimeChargerTriggerCondition(values[1],
						triggerConditionFromString(values[2], ";") as TimeTriggerCondition,
						triggerConditionFromString(values[3], ";") as ChargerTriggerCondition
					)
				else -> {
					null
				}
			}
		}

		fun triggerFromString(raw: String): Trigger {
			val values = raw.split(";;")
			if (values.size < 2) throw IllegalStateException("invalid value $raw")
			val t = when (values[0]) {
				"charger", "time", "tcdual" -> {
					triggerConditionFromString(raw)
				}
				else -> {
					null
				}
			}
			if (t != null) return t
			throw IllegalStateException("invalid trigger type ${values[0]}")
		}

		fun conditionFromString(raw: String): Condition {
			val values = raw.split(";;")
			if (values.size < 2) throw IllegalStateException("invalid value $raw")
			val t = when (values[0]) {
				"charger", "time", "tcdual" -> {
					triggerConditionFromString(raw)
				}
				else -> {
					null
				}
			}
			if (t != null) return t
			throw IllegalStateException("invalid condition type ${values[0]}")
		}
	}
}

interface Trigger {
	val id: String
	fun setup(applicationContext: Context, service: WellbeingService)
	fun dispose(applicationContext: Context, service: WellbeingService)
	fun toString(seperator: String): String
}

interface Condition {
	val id: String
	fun isFulfilled(applicationContext: Context, service: WellbeingService): Boolean
	fun toString(seperator: String): String
}

interface TriggerCondition : Trigger, Condition

abstract class Container(override val id: String, val name: String, val sep: String) : TriggerCondition {
	abstract val a: Array<TriggerCondition>

	fun onTrigger(condition: (Trigger) -> Boolean): Trigger? {
		a.forEach { if (condition(it)) return it }
		return null
	}

	override fun setup(applicationContext: Context, service: WellbeingService) {
		a.forEach { it.setup(applicationContext, service) }
	}

	override fun dispose(applicationContext: Context, service: WellbeingService) {
		a.forEach { it.dispose(applicationContext, service) }
	}

	override fun isFulfilled(applicationContext: Context, service: WellbeingService): Boolean {
		a.forEach {
			if (it.isFulfilled(applicationContext, service)) return true }
		return false
	}

	override fun toString(seperator: String): String {
		var s = name + seperator + id
		a.forEach { s += seperator + it.toString(sep) }
		return s
	}
}

open class TimeTriggerCondition(
	override val id: String,
	val startHour: Int,
	val startMinute: Int,
	val endHour: Int,
	val endMinute: Int,
	val weekdays: BooleanArray // length = 7, 0 = monday, 6 = sunday
) : TriggerCondition {
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
		ScheduleUtils.setAlarm(applicationContext, id, start)
		ScheduleUtils.setAlarm(applicationContext, "expire::$id", end)
	}

	override fun dispose(applicationContext: Context, service: WellbeingService) {
		ScheduleUtils.dropAlarm(applicationContext, id)
		ScheduleUtils.dropAlarm(applicationContext, "expire::$id")
	}

	override fun toString(seperator: String): String {
		var bits = 0; for (i in weekdays.indices) if (weekdays[i]) bits = bits or (1 shl i) // boolean[] -> bitmask
		return "time$seperator$id$seperator$startHour$seperator$startMinute$seperator$endHour$seperator$endMinute$seperator$bits"
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
		})
	}
}

class ChargerTriggerCondition(override val id: String): TriggerCondition {
	override fun setup(applicationContext: Context, service: WellbeingService) {
		/* No setup needed, onPowerConnectionReceived() is all we need */
	}

	override fun dispose(applicationContext: Context, service: WellbeingService) {
		/* No disposal needed as we don't setup anything */
	}

	override fun toString(seperator: String): String {
		return "charger$seperator$id"
	}

	override fun isFulfilled(applicationContext: Context, service: WellbeingService): Boolean {
		val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { applicationContext.registerReceiver(null, it) }

		val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
		return chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
				chargePlug == BatteryManager.BATTERY_PLUGGED_AC
	}
}

class TimeChargerTriggerCondition(
	override val id: String,
	val timeTriggerCondition: TimeTriggerCondition,
	chargerCondition: ChargerTriggerCondition
) : Container(id, "tcdual", ";") {
	override val a: Array<TriggerCondition> = arrayOf(timeTriggerCondition, chargerCondition)
}