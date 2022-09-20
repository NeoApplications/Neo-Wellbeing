package org.eu.droid_ng.wellbeing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.eu.droid_ng.wellbeing.lib.AppTimersInternal;

public class BootReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		if (!"android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
			/* Make sure no one is trying to fool us */
			return;
		}
		AppTimersInternal.get(context).onBootRecieved();
	}
}