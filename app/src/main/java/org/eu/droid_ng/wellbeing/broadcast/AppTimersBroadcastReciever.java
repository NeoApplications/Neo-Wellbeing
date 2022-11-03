package org.eu.droid_ng.wellbeing.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.eu.droid_ng.wellbeing.lib.WellbeingService;

public class AppTimersBroadcastReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		WellbeingService tw = WellbeingService.get();
		tw.onAppTimerExpired(
						intent.getIntExtra("observerId", -1),
						intent.getStringExtra("uniqueObserverId"));
	}
}