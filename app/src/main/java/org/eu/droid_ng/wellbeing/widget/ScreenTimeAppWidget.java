package org.eu.droid_ng.wellbeing.widget;

import android.app.PendingIntent;
import android.app.usage.UsageStatsManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.Utils;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.time.Duration;

public class ScreenTimeAppWidget extends AppWidgetProvider {
    private static final int[] appViewIds = new int[]{
            R.id.appwidget_app1_n, R.id.appwidget_app2_n, R.id.appwidget_app3_n
    };
    private static final int[] appView2Ids = new int[]{
            R.id.appwidget_app1_t, R.id.appwidget_app2_t, R.id.appwidget_app3_t
    };
    private static final int[] appView3Ids = new int[]{
            R.id.appwidget_app1_l, R.id.appwidget_app2_l, R.id.appwidget_app3_l
    };
    private PendingIntent pendingIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if ("org.eu.droid_ng.wellbeing.APPWIDGET_UPDATE".equals(intent.getAction())) {
            AppWidgetManager awm = AppWidgetManager.getInstance(context);
            onUpdate(context, awm, awm.getAppWidgetIds(new ComponentName(context, ScreenTimeAppWidget.class)));
        }
    }

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
                remoteViews.setViewVisibility(appView3Ids[i], View.GONE);
                remoteViews.setViewVisibility(appViewIds[i], View.GONE);
            } else {
                remoteViews.setViewVisibility(appViewIds[i], View.VISIBLE);
                remoteViews.setViewVisibility(appView3Ids[i], View.VISIBLE);
                String packageName = mostUsedPackages[i];
                String packageLabel = packageName;
                try {
                    packageLabel = WellbeingService.get()
                            .getApplicationLabel(packageName).toString();
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e("ScreenTimeAppWidget", "Failed to get app label!");
                }

                remoteViews.setTextViewText(appViewIds[i], packageLabel);
                remoteViews.setTextViewText(appView2Ids[i], formatDuration(Utils.getTimeUsed(usm, packageName)));
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