package org.eu.droid_ng.wellbeing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		AppTimersInternal.get(context).onBootRecieved();
	}
}