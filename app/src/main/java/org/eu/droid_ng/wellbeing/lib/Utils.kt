package org.eu.droid_ng.wellbeing.lib

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.util.Log
import java.time.*
import java.util.*

object Utils {
    private var calculatedUsageStats: HashMap<String, Duration>? = null

    private fun eventsStr(events: Iterable<UsageEvents.Event>): String {
        val b = StringBuilder()
        b.append("[")
        for (element in events) {
            b.append("el(t=").append(element.eventType).append("), ")
        }
        b.replace(b.length - 2, b.length - 1, "]")
        return b.toString()
    }

    @JvmStatic
    fun clearUsageStatsCache(usm: UsageStatsManager?, recalculate: Boolean) {
        calculatedUsageStats = null
        if (recalculate) getTimeUsed(usm!!, arrayOfNulls(0))
    }

    @JvmStatic
    fun getTimeUsed(usm: UsageStatsManager, packageNames: Array<String?>): Duration? {
        /*
		 * When writing this code, I learnt a lesson. UsageStats and UsageEvents APIs are fucking dumb.
		 * I had cases of user opening the app 3 times and closing it 2 times, cases of user opening the app 2 times without closing it at all...
		 * But in the very end this works. And it's about 3 trillion times faster than UsageStatsManager queries.
		 */
        if (calculatedUsageStats == null) { // Cache not available. Calculate it once and keep it.
            val z = ZoneId.systemDefault()
            val startTime = LocalDateTime.of(LocalDate.now(z), LocalTime.MIDNIGHT).atZone(z)
                .toEpochSecond() * 1000
            val usageEvents: UsageEvents = usm.queryEvents(startTime, System.currentTimeMillis())
            var currentEvent: UsageEvents.Event
            val e = HashMap<String, ArrayList<UsageEvents.Event>>()
            while (usageEvents.hasNextEvent()) {
                currentEvent = UsageEvents.Event()
                usageEvents.getNextEvent(currentEvent)
                if (currentEvent.eventType == UsageEvents.Event.ACTIVITY_PAUSED ||
                    currentEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                ) {
                    e.computeIfAbsent(
                        currentEvent.packageName
                    ) { ArrayList() }
                        .add(currentEvent)
                }
            }
            calculatedUsageStats = HashMap<String, Duration>()
            e.forEach { (pkgName: String, events: ArrayList<UsageEvents.Event>) ->
                var i = 0
                while (i < events.size) {
                    var j = 1
                    if (i + j >= events.size) {
                        i += 2
                        continue
                    }
                    val eventOne = events[i]
                    var eventTwo = events[i + j]
                    if (eventOne.eventType == UsageEvents.Event.ACTIVITY_PAUSED) {
                        Log.e("AppTimersInternal", "usm soft assert1 fail!! eventOneType=" + eventOne.eventType + " eventTwoType=" + eventTwo.eventType)
                        i += 1
                        continue
                        // Unlucky case. Skip one. Should never happen, but safe is safe. edit: happens. help me
                    }
                    if (eventOne.eventType != UsageEvents.Event.ACTIVITY_RESUMED) {
                        Log.e("AppTimersInternal", "usm soft assert2 fail!! pkgName=$pkgName i=$i j=$j events=${eventsStr(events)}")
                        i += 2
                        continue
                        // did not find start
                    }
                    while (events[i + j].also { eventTwo = it }.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                        j++
                        if (i + j == events.size) {
                            Log.e("AppTimersInternal", "usm soft assert3 fail!! pkgName=$pkgName i=$i j=$j events=${eventsStr(events)}")
                            break
                            // did not find ending
                        }
                    }
                    if (eventTwo.eventType != UsageEvents.Event.ACTIVITY_PAUSED) {
                        Log.e("AppTimersInternal", "usm soft assert4 fail!! pkgName=$pkgName i=$i j=$j events=${eventsStr(events)}")
                        i += 2
                        continue
                        // did not find ending
                    }
                    calculatedUsageStats!![pkgName] = calculatedUsageStats!!.getOrDefault(pkgName, Duration.ZERO)
                        .plus(Duration.ofMillis(eventTwo.timeStamp - eventOne.timeStamp))
                    i += 2
                }
            }
        }
        var d = Duration.ZERO
        for (packageName in packageNames) {
            d = d.plus(calculatedUsageStats!!.getOrDefault(packageName, Duration.ZERO))
        }
        return if (d.isNegative) null else d
    }
}