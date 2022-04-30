package org.eu.droid_ng.wellbeing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NotificationBroadcastReciever extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		final WellbeingStateClient client = new WellbeingStateClient(context);
		if (GlobalWellbeingState.INTENT_ACTION_TAKE_BREAK.equals(intent.getAction())) {
			client.doBindService(boundService -> boundService.state.takeBreak(boundService.state.notificationBreakTime));
		} else if (GlobalWellbeingState.INTENT_ACTION_QUIT_BREAK.equals(intent.getAction())) {
			client.doBindService(boundService -> boundService.state.endBreak());
		} else if (GlobalWellbeingState.INTENT_ACTION_QUIT_FOCUS.equals(intent.getAction())) {
			client.doBindService(boundService -> boundService.state.disableFocusMode(), false, true, true);
		} else if (GlobalWellbeingState.INTENT_ACTION_UNSUSPEND_ALL.equals(intent.getAction())) {
			client.doBindService(boundService -> boundService.state.manualUnsuspend(null, true));
		}
	}
}