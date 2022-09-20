package org.eu.droid_ng.wellbeing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.eu.droid_ng.wellbeing.lib.AppTimersInternal;
import org.eu.droid_ng.wellbeing.lib.TransistentWellbeingState;

public class AppTimersBroadcastReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		new TransistentWellbeingState(context).onAppTimerExpired(
						intent.getIntExtra("observerId", -1),
						intent.getStringExtra("uniqueObserverId"));
	}
}