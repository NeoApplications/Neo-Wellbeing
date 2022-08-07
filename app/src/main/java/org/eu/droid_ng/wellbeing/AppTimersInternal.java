package org.eu.droid_ng.wellbeing;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Load AppTimers state from disk and hold it in a instance. Allow easy configuration with API calls and make this handle all the specifics.
 * Architecture notes:
 *
 * App timers must not depend on our GlobalWellbeingState service infrastructure.
 * Android calls an Intent which we can use for a quick BroadcastReciever to
 * suspend the app. However, we have an limit of 1000 observers and we must provide the id mapping for them.
 * Use SharedPreferences to store known observers (uoid) and their ids (oid). When readding observer, substract timeUsed.
 *
 */
public class AppTimersInternal {
	@SuppressLint("StaticFieldLeak")
	private static AppTimersInternal instance;

	private final Context ctx;
	private final PackageManager pm;
	private final PackageManagerDelegate pmd;
	private final UsageStatsManager usm;
	private final SharedPreferences prefs; // uoid <-> oid map
	private final SharedPreferences config;
	private HashMap<String, Duration> calculatedUsageStats = null;

	private AppTimersInternal(Context ctx) {
		this.ctx = ctx;
		this.pm = ctx.getPackageManager();
		this.pmd = new PackageManagerDelegate(pm);
		this.usm = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
		this.prefs = ctx.getSharedPreferences("AppTimersInternal", 0);
		this.config = ctx.getSharedPreferences("appTimers", 0);
	}

	public static AppTimersInternal get(Context ctx) {
		if (instance == null) {
			instance = new AppTimersInternal(ctx);
		}
		return instance;
	}

	// start time limit core
	private void updatePrefs(String key, int value) {
		if (value < 0) {
			prefs.edit().remove(key).apply();
		} else {
			prefs.edit().putInt(key, value).apply();
		}
	}

	private int makeOid() {
		Collection<?> vals = prefs.getAll().values();
		// try to save time by starting at size value
		for (int i = vals.size(); i < 1000; i++) {
			if (!vals.contains(i))
				return i;
		}
		// if all high values are used up, try all values
		for (int i = 0; i < 1000; i++) {
			if (!vals.contains(i))
				return i;
		}
		// cant handle this
		throw new IllegalStateException("more than 1000 observers registered");
	}

	private static final class ParsedUoid {
		public final String action;
		public final long timeMillis;
		public final String[] pkgs;

		public ParsedUoid(String action, long timeMillis, String[] pkgs) {
			this.action = action;
			this.timeMillis = timeMillis;
			this.pkgs = pkgs;
		}

		@NonNull
		@Override
		public String toString() {
			return action + ":" + timeMillis + "//" + String.join(":", pkgs);
		}

		public static ParsedUoid from(String uoid) {
			int l = uoid.indexOf(":");
			int ll = uoid.indexOf("//");
			String action = uoid.substring(0, l);
			long timeMillis = Long.parseLong(uoid.substring(l + 1, ll));
			String[] pkgs = uoid.substring(ll + 2).split(":");
			return new ParsedUoid(action, timeMillis, pkgs);
		}
	}

	private void setUnhintedAppTimerInternal(Integer oid, String uoid, String[] toObserve, Duration timeLimit) {
		Intent i = new Intent(ctx, AppTimersBroadcastReciever.class);
		i.putExtra("observerId", oid);
		i.putExtra("uniqueObserverId", uoid);
		PendingIntent pintent = PendingIntent.getBroadcast(ctx, oid, i, PendingIntent.FLAG_IMMUTABLE);
		PackageManagerDelegate.registerAppUsageObserver(usm, oid, toObserve, timeLimit.toMillis(), TimeUnit.MILLISECONDS, pintent);
	}

	private void setHintedAppTimerInternal(Integer oid, String uoid, String[] toObserve, Duration timeLimit, Duration timeUsed) {
		Intent i = new Intent(ctx, AppTimersBroadcastReciever.class);
		i.putExtra("observerId", oid);
		i.putExtra("uniqueObserverId", uoid);
		PendingIntent pintent = PendingIntent.getBroadcast(ctx, oid, i, PendingIntent.FLAG_IMMUTABLE);
		PackageManagerDelegate.registerAppUsageLimitObserver(usm, oid, toObserve, timeLimit, timeUsed, pintent);
	}

	private void setAppTimerInternal(String uoid, String[] toObserve, Duration timeLimit, @Nullable Duration timeUsed) {
		int oid = prefs.getInt(uoid, -1);
		if (timeUsed == null) {
			setUnhintedAppTimerInternal(oid, uoid, toObserve, timeLimit);
		} else {
			setHintedAppTimerInternal(oid, uoid, toObserve, timeLimit, timeUsed);
		}
	}
	// end time limit core

	// start AppTimer feature
	private void setAppTimer(String[] toObserve, Duration timeLimit, @Nullable Duration timeUsed) {
		// AppLimit: do not provide info to launcher, use registerAppUsageObserver
		// AppTimer: provide info to launcher, use registerAppUsageLimitObserver
		String uoid = new ParsedUoid(timeUsed == null ? "AppLimit" : "AppTimer", timeLimit.toMillis(), toObserve).toString();
		Duration timeLimitInternal = timeLimit;
		if (timeUsed != null) {
			timeLimitInternal = timeLimitInternal.minus(timeUsed);
		}
		if (!prefs.contains(uoid)) {
			updatePrefs(uoid, makeOid());
		}
		setAppTimerInternal(uoid, toObserve, timeLimitInternal, timeUsed);
	}

	private void dropAppTimer(ParsedUoid parsedUoid) {
		String uoid = parsedUoid.toString();
		updatePrefs(uoid, -1); //delete pref
		if (parsedUoid.action.equals("AppTimer")) {
			PackageManagerDelegate.unregisterAppUsageLimitObserver(usm, prefs.getInt(uoid, -1));
		} else {
			PackageManagerDelegate.unregisterAppUsageObserver(usm, prefs.getInt(uoid, -1));
		}
	}

	private void resetupAppTimerPreference(String packageName) {
		String[] s = new String[]{ packageName };
		setAppTimer(s, Duration.ofMinutes(config.getInt(packageName, -1)), getTimeUsed(s));
	}

	public Duration getTimeUsed(String[] packageNames) {
		/*
		 * When writing this code, I learnt a lesson. UsageStats and UsageEvents APIs are fucking dumb.
		 * I had cases of user opening the app 3 times and closing it 2 times, cases of user opening the app 2 times without closing it at all...
		 * But in the very end this works. And it's about 3 trillion times faster than UsageStatsManager queries.
		 */
		if (calculatedUsageStats == null) {
			ZoneId z = ZoneId.systemDefault();
			long startTime = LocalDateTime.of(LocalDate.now(z), LocalTime.MIDNIGHT).atZone(z).toEpochSecond()*1000;
			UsageEvents usageEvents = usm.queryEvents(startTime, System.currentTimeMillis());
			UsageEvents.Event currentEvent;
			HashMap<String, ArrayList<UsageEvents.Event>> e = new HashMap<>();

			while (usageEvents.hasNextEvent()) {
				currentEvent = new UsageEvents.Event();
				usageEvents.getNextEvent(currentEvent);
				if (currentEvent.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED ||
						currentEvent.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
					e.computeIfAbsent(currentEvent.getPackageName(), p -> new ArrayList<>())
							.add(currentEvent);
				}
			}

			calculatedUsageStats = new HashMap<>();

			e.forEach((pkgName, events) -> {
				for (int i = 0; i < events.size(); i+=2) {
					int j = 1;
					if (i+j >= events.size())
						continue;
					UsageEvents.Event eventOne = events.get(i);
					UsageEvents.Event eventTwo = events.get(i+j);
					if (eventOne.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED) {
						i -= 1;
						Log.e("AppTimersInternal", "usm soft assert1 fail!! eventOneType=" + eventOne.getEventType() + " eventTwoType=" + eventTwo.getEventType());
						continue;
						// Unlucky case. Skip one. Should never happen, but safe is safe. edit: happens. help me
					}
					if (!(eventOne.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED)) { // where tf is start
						StringBuilder b = new StringBuilder(); //print array, tostring doesnt work, usageevent has no tostring
						b.append("[");
						for (UsageEvents.Event element : events) {
							b.append("el(t=").append(element.getEventType()).append("), ");
						}
						b.replace(b.length()-2, b.length()-1, "]");
						throw new IllegalStateException("usm hard assert1 fail!! pkgName=" + pkgName + " i=" + i + " j=" +  j + " events=" + b);
					}
					while ((eventTwo = events.get(i+j)).getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
						j++;
						if (i+j==events.size()) { //array oob - didnt find ending??
							StringBuilder b = new StringBuilder(); //print array, tostring doesnt work, usageevent has no tostring
							b.append("[");
							for (UsageEvents.Event element : events) {
								b.append("el(t=").append(element.getEventType()).append("), ");
							}
							b.replace(b.length()-2, b.length()-1, "]");
							throw new IllegalStateException("usm hard assert2 fail!! pkgName=" + pkgName + " i=" + i + " j=" +  j + " events=" + b);
						}
					}
					if (!(eventTwo.getEventType() == UsageEvents.Event.ACTIVITY_PAUSED)) { // didnt find ending
						StringBuilder b = new StringBuilder(); //print array, tostring doesnt work, usageevent has no tostring
						b.append("[");
						for (UsageEvents.Event element : events) {
							b.append("el(t=").append(element.getEventType()).append("), ");
						}
						b.replace(b.length()-2, b.length()-1, "]");
						throw new IllegalStateException("usm hard assert3 fail!! pkgName=" + pkgName + " i=" + i + " j=" +  j + " events=" + b);
					}

					calculatedUsageStats.put(pkgName, Objects.requireNonNull(calculatedUsageStats.getOrDefault(pkgName, Duration.ZERO)).plus(Duration.ofMillis(eventTwo.getTimeStamp() - eventOne.getTimeStamp())));
				}
			});
		}

		Duration d = Duration.ZERO;
		for (String packageName : packageNames) {
			d = d.plus(calculatedUsageStats.getOrDefault(packageName, Duration.ZERO));
		}
		if (d.isNegative())
			return null;
		return d;
	}

	public void onUpdateAppTimerPreference(String packageName, Duration oldLimit, Duration limit) {
		String[] s = new String[]{ packageName };
		ParsedUoid u = new ParsedUoid("AppTimer", oldLimit.toMillis(), s);
		if (!prefs.contains(u.toString()))
			u = new ParsedUoid("AppLimit", oldLimit.toMillis(), s);
		dropAppTimer(u);
		if (limit.toMillis() > 0)
			setAppTimer(s, limit, getTimeUsed(s));
	}

	public void onBootRecieved() {
		prefs.edit().clear().apply();
		for (String pkgName : config.getAll().keySet()) {
			resetupAppTimerPreference(pkgName);
		}
	}

	public void onBroadcastRecieve(Integer oid, String uoid) {
		if (!Objects.equals(prefs.getInt(uoid, -2), oid)) {
			Toast.makeText(ctx, "AppTimersInternal: unknown oid/uoid - " + oid + " / " + uoid, Toast.LENGTH_LONG).show();
			return;
		}
		ParsedUoid parsed = ParsedUoid.from(uoid);
		dropAppTimer(parsed);
		// Actual logic starting here please
		//TODO: suspend & break logic
		Toast.makeText(ctx, "AppTimersInternal: success oid:" + oid + " action:" + parsed.action + " timeMillis:" + parsed.timeMillis + " pkgs:" + String.join(",", parsed.pkgs), Toast.LENGTH_LONG).show();
	}
	// end AppTimer feature
}
