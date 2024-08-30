package org.eu.droid_ng.wellbeing.lib

import android.annotation.SuppressLint
import android.app.usage.UsageEvents
import android.app.usage.UsageEvents.Event
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.Log
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate
import java.time.*
import java.util.*


object Utils {
    private const val MOST_USED_PKG_CACHE_SIZE: Int = 3
    private const val MOST_USED_PKG_MIN_USAGE_MINS: Long = 5
    private var calculatedUsageStats: Map<String, Duration>? = null
    private var calculatedScreenTime: Duration? = null
    private var mostUsedPackages: Array<String>? = null
    const val PACKAGE_MANAGER_MATCH_INSTANT = 0x00800000
    val blackListedPackages: HashSet<String> = HashSet()
    val restrictedPackages: HashSet<String> = HashSet()

    private fun eventsStr(events: Iterable<Event>): String {
        val b = StringBuilder()
        b.append("[")
        for (element in events) {
            b.append("el(t=").append(element.eventType).append("), ")
        }
        b.replace(b.length - 2, b.length - 1, "]")
        return b.toString()
    }

    fun clearUsageStatsCache(usm: UsageStatsManager?, pm: PackageManager?, pmd: PackageManagerDelegate?, recalculate: Boolean) {
        calculatedUsageStats = null
        calculatedScreenTime = null
        mostUsedPackages = null
        if (recalculate) {
            updateApplicationBlackLists(pm!!, pmd!!)
            checkInitializeCache(usm!!)
        }
    }

    fun getTimeUsed(usm: UsageStatsManager, packageName: String?): Duration {
        checkInitializeCache(usm)
        return calculatedUsageStats!!.getOrDefault(packageName, Duration.ZERO)
    }

    fun getTimeUsed(usm: UsageStatsManager, packageNames: Array<String?>): Duration {
        checkInitializeCache(usm)
        var d = Duration.ZERO
        for (packageName in packageNames) {
            d = d.plus(calculatedUsageStats!!.getOrDefault(packageName, Duration.ZERO))
        }
        return if (d.isNegative) Duration.ZERO else d
    }

    fun getScreenTime(usm: UsageStatsManager): Duration {
        checkInitializeCache(usm)
        return calculatedScreenTime!!
    }

    fun getMostUsedPackages(usm: UsageStatsManager): Array<String> {
        checkInitializeCache(usm)
        return mostUsedPackages!!
    }

    private fun checkInitializeCache(usm: UsageStatsManager) {
        if (calculatedUsageStats != null) return
        // Cache not available. Calculate it once and keep it.
        val z = ZoneId.systemDefault()
        val startTime = LocalDateTime.now().with(LocalTime.MIN) // Start of day
            .atZone(z).toEpochSecond() * 1000
        val result = calculateUsageStats(usm, startTime, System.currentTimeMillis())
        calculatedScreenTime = result.first
        calculatedUsageStats = result.second.first
        mostUsedPackages = result.second.second
    }

    fun calculateUsageStats(usm: UsageStatsManager, startTimeMillis: Long, endTimeMillis: Long): Pair<Duration, Pair<Map<String, Duration>, Array<String>>> {
        val usageEvents: UsageEvents = usm.queryEvents(startTimeMillis, endTimeMillis)
        var currentEvent: Event
        val e = HashMap<String, ArrayList<Event>>()
        while (usageEvents.hasNextEvent()) {
            currentEvent = Event()
            usageEvents.getNextEvent(currentEvent)
            e.computeIfAbsent(currentEvent.packageName) { ArrayList() }.add(currentEvent)
        }
        // Calculate usageStats
        val myCalculatedUsageStats = HashMap<String, Duration>()
        e.forEach { (pkgName: String, events: ArrayList<Event>) ->
            val openActivities = hashMapOf<String, Long>()
            for (event in events.sortedWith { a, b ->
                val c = a.timeStamp.compareTo(b.timeStamp)
                if (c != 0) return@sortedWith c
                val d = a.eventType == Event.ACTIVITY_RESUMED || a.eventType == 4 /* CONTINUE_PREVIOUS_DAY */
                val f = b.eventType == Event.ACTIVITY_RESUMED || b.eventType == 4 /* CONTINUE_PREVIOUS_DAY */
                return@sortedWith d.compareTo(f)
            }) {
                when (event.eventType) {
                    Event.ACTIVITY_PAUSED -> {
                        val start = openActivities.remove(event.className)
                        if (start != null)
                            myCalculatedUsageStats[pkgName] = myCalculatedUsageStats
                                .getOrDefault(pkgName, Duration.ZERO).plus(
                                    Duration.ofMillis(event.timeStamp - start))
                        else
                            Log.w("WellbeingUtils", "got ACTIVITY_PAUSED for ${event.className} at ${event.timeStamp} but didn't remember it starting")
                    }
                    Event.DEVICE_SHUTDOWN, 3 /* END_OF_DAY */ -> {
                        while (openActivities.isNotEmpty()) {
                            myCalculatedUsageStats[pkgName] = myCalculatedUsageStats
                                .getOrDefault(pkgName, Duration.ZERO).plus(
                                    Duration.ofMillis(event.timeStamp -
                                            openActivities.remove(openActivities.keys.first())!!
                                    ))
                        }
                    }
                    Event.ACTIVITY_RESUMED, 4 /* CONTINUE_PREVIOUS_DAY */ ->
                        openActivities[event.className] = event.timeStamp
                }
            }
        }
        // Calculate screenTime + mostUsedPackages
        var screenTimeTmp: Duration = Duration.ZERO
        val mostUsedPackagesTmp = arrayOfNulls<String>(MOST_USED_PKG_CACHE_SIZE)
        val mostUsedPackageTime = Array(MOST_USED_PKG_CACHE_SIZE) { MOST_USED_PKG_MIN_USAGE_MINS }
        myCalculatedUsageStats.forEach { (pkgName: String, duration: Duration) ->
            val seconds: Long
            if (!blackListedPackages.contains(pkgName)) {
                screenTimeTmp = screenTimeTmp.plus(duration)
                seconds = duration.seconds
            } else seconds = 0
            if (!restrictedPackages.contains(pkgName) && seconds > mostUsedPackageTime[MOST_USED_PKG_CACHE_SIZE - 1]) {
                var index = 0
                while (seconds <= mostUsedPackageTime[index]) {
                    index++
                }
                System.arraycopy(mostUsedPackagesTmp, index,
                        mostUsedPackagesTmp, index + 1,
                        (MOST_USED_PKG_CACHE_SIZE - 1) - index)
                System.arraycopy(mostUsedPackageTime, index,
                        mostUsedPackageTime, index + 1,
                        (MOST_USED_PKG_CACHE_SIZE - 1) - index)
                mostUsedPackagesTmp[index] = pkgName
                mostUsedPackageTime[index] = seconds
            }
        }
        val myMostUsedPackages: Array<String>
        if (mostUsedPackagesTmp[MOST_USED_PKG_CACHE_SIZE - 1] != null) {
            @Suppress("UNCHECKED_CAST")
            myMostUsedPackages = mostUsedPackagesTmp as Array<String>
        } else if (mostUsedPackagesTmp[0] == null) {
            myMostUsedPackages = emptyArray()
        } else {
            var arraySize = MOST_USED_PKG_CACHE_SIZE
            while (arraySize --> 0) {
                if (mostUsedPackagesTmp[arraySize] != null) {
                    arraySize + 1
                    break
                }
            }
            @Suppress("UNCHECKED_CAST")
            myMostUsedPackages = mostUsedPackagesTmp.copyOf(arraySize) as Array<String>
        }
        return Pair(screenTimeTmp, Pair(myCalculatedUsageStats, myMostUsedPackages))
    }

    @SuppressLint("DiscouragedApi")
    private fun updateApplicationBlackLists(pm: PackageManager, pmd: PackageManagerDelegate) {
        blackListedPackages.clear()
        restrictedPackages.clear()

        blackListedPackages.add("com.android.systemui")

        val resId = Resources.getSystem().getIdentifier(
                "config_recentsComponentName", "string", "android")
        if (resId != 0) {
            val recentsComponent = ComponentName.unflattenFromString(
                    Resources.getSystem().getString(resId))
            if (recentsComponent != null)
                restrictedPackages.add(recentsComponent.packageName)
        }
        var intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        addDefaultHandlersToBlacklist(pm, intent, restrictedPackages)
        restrictedPackages.addAll(blackListedPackages)
        restrictedPackages.add("com.android.settings")
        // Add every system dialer to the blacklist
        intent = Intent(Intent.ACTION_DIAL)
        intent.addCategory(Intent.CATEGORY_DEFAULT)
        addDefaultHandlersToBlacklist(pm, intent, restrictedPackages)
        restrictedPackages.add("org.eu.droid_ng.wellbeing")
        //Log.d("Utils", "Hard Blacklisted packages: $blackListedPackages")
        //Log.d("Utils", "Soft Blacklisted packages: $restrictedPackages")
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA).map { it.packageName }.toTypedArray()
        restrictedPackages.addAll(pmd.getUnsuspendablePackages(packages))
    }

    private fun addDefaultHandlersToBlacklist(pm: PackageManager, intent: Intent, blacklist: HashSet<String>) {
        // Add the system handlers to the blacklist
        val resolveInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_SYSTEM_ONLY)
        if (resolveInfoList.isNotEmpty()) {
            for (resolveInfo in resolveInfoList) {
                blacklist.add(resolveInfo.activityInfo.packageName)
            }
        }
        // Add the default handler to the blacklist
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (resolveInfo != null) {
            blacklist.add(resolveInfo.activityInfo.packageName)
        }
    }
}