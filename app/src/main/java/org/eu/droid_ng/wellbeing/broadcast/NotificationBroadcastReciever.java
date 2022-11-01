package org.eu.droid_ng.wellbeing.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.eu.droid_ng.wellbeing.lib.TransistentWellbeingState;

public class NotificationBroadcastReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		TransistentWellbeingState.use(context, tw ->
				tw.onNotificationActionClick(intent.getAction()));
	}
}