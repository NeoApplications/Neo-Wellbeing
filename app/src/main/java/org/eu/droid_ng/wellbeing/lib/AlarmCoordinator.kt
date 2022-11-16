package org.eu.droid_ng.wellbeing.lib

import android.app.AlarmManager
import android.content.Context
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmCoordinator(private val context: Context) {
	fun updateState() {
		val am = context.getSystemService(AlarmManager::class.java)
		val next = am.nextAlarmClock
		if (next == null) {
			ScheduleUtils.dropAlarm(context, "alc", am)
		} else {
			ScheduleUtils.setAlarm(context, "alc", LocalDateTime.ofInstant(Instant.ofEpochMilli(next.triggerTime), ZoneId.systemDefault()), am)
		}
	}

	fun fired() {
		WellbeingService.get().doTrigger(true) { it is TimeChargerTriggerCondition && it.endOnAlarm }
	}
}