package org.eu.droid_ng.wellbeing.widget;

import android.app.PendingIntent;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.RemoteViews;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.Utils;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.time.Duration;

public class ScreenTimeAppWidget extends AppWidgetProvider {
    private static final int[] appViewIds = new int[]{
            R.id.appwidget_app1, R.id.appwidget_app2, R.id.appwidget_app3
    };
    private PendingIntent pendingIntent;

    private void checkInitialize(Context context) {
        if (pendingIntent == null) {
            Intent intent = new Intent("com.android.settings.action.IA_SETTINGS");
            intent.setPackage(context.getPackageName());
            pendingIntent = PendingIntent.getActivity(context, this.hashCode(),
                    intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    @Override
    public void onEnabled(Context context) {
        checkInitialize(context);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        checkInitialize(context);
        Utils.clearUsageStatsCache(WellbeingService.get().usm, context.getPackageManager(), true);

        for (int appWidgetId : appWidgetIds) {
            appWidgetManager.updateAppWidget(appWidgetId,
                    updateLayout(context, appWidgetManager, appWidgetId));
        }
    }

    public void onDeleted(Context context, int[] appWidgetIds) {

    }

    @Override
    public void onDisabled(Context context) {

    }

    private RemoteViews updateLayout(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        UsageStatsManager usm = WellbeingService.get().usm;
        RemoteViews remoteViews = new RemoteViews(
                context.getPackageName(), R.layout.appwidget_screen_time);

        remoteViews.setOnClickPendingIntent(R.id.appwidget_root, pendingIntent);
        remoteViews.setTextViewText(R.id.appwidget_screen_time,
                formatDuration(Utils.getScreenTime(usm)));
        String[] mostUsedPackages = Utils.getMostUsedPackages(usm);
        for (int i = 0; i < appViewIds.length; i++) {
            if (i >= mostUsedPackages.length) {
                remoteViews.setTextViewText(appViewIds[i], "");
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                String packageName = mostUsedPackages[i];
                String packageLabel = packageName;
                try {
                    packageLabel = WellbeingService.get()
                            .getApplicationLabel(packageName).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("ScreenTimeAppWidget", "Failed to get app label!");
                }
                stringBuilder.append(packageLabel).append(" ").append(
                        formatDuration(Utils.getTimeUsed(usm, packageName)));

                remoteViews.setTextViewText(appViewIds[i], stringBuilder.toString());
            }
        }
        return remoteViews;
    }

    private static String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes();
        minutes -= (hours * 60);
        if (hours == 0) {
            return minutes + "m";
        } else if (minutes == 0) {
            return hours + "h";
        } else {
            return hours + "h " +
                    minutes + "m";
        }
    }
}