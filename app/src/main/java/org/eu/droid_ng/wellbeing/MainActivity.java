package org.eu.droid_ng.wellbeing;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.eu.droid_ng.wellbeing.PackageManagerDelegate.SuspendDialogInfo;

public class MainActivity extends Activity {
	private final WellbeingStateClient client = new WellbeingStateClient(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PackageManagerDelegate delegate = new PackageManagerDelegate(getPackageManager());
		findViewById(R.id.button).setOnClickListener(a -> {
			client.startService();
			client.doBindService(boundService -> {
				boundService.state.type = GlobalWellbeingState.SERVICE_TYPE.TYPE_FOCUS_MODE;
				boundService.updateNotification(R.string.focus_mode, R.string.notification_focus_mode, R.drawable.ic_stat_name, new Notification.Action[]{
						boundService.buildAction(R.string.focus_mode_break, R.drawable.ic_take_break, new Intent(MainActivity.this, MainActivity.class)),
						boundService.buildAction(R.string.focus_mode_off, R.drawable.ic_stat_name, new Intent(MainActivity.this, MainActivity.class))
				}, new Intent(MainActivity.this, MainActivity.class));
				boundService.state.focusModeSuspend();
			});
		});
		findViewById(R.id.button2).setOnClickListener(a -> {
			client.doBindService(boundService -> {
				boundService.state.focusModeUnsuspend();
				client.killService();
			}, true);
			for (String s : delegate.setPackagesSuspended(new String[]{"org.lineageos.jelly","org.lineageos.eleven"},false,null,null,null)) {
				Log.e("OpenWellbeing", "failed: " + s);
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		client.doUnbindService();
	}
}