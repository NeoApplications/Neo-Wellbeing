package org.eu.droid_ng.wellbeing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ManuallyUnsuspendBroadcastReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		final WellbeingStateClient client = new WellbeingStateClient(context);
		if (!"android.intent.action.PACKAGE_UNSUSPENDED_MANUALLY".equals(intent.getAction())) {
			/* Make sure no one is trying to fool us */
			return;
		}
		final String packageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME);
		if (packageName == null) {
			/* Make sure we have a package name */
			Toast.makeText(context, "Assertion failure (0xAC): packageName is null. Please report this to the developers!", Toast.LENGTH_LONG).show();
			return;
		}

		client.doBindService(boundService -> {
			boundService.state.reasonMap.remove(packageName);
			Toast.makeText(context, "Manually unsuspended: " + packageName + " " + boundService.state.reasonMap.getOrDefault(packageName, GlobalWellbeingState.REASON.REASON_UNKNOWN), Toast.LENGTH_LONG).show();
		}, true);
	}
}