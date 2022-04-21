package org.eu.droid_ng.wellbeing;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.eu.droid_ng.wellbeing.PackageManagerDelegate.SuspendDialogInfo;

public class MainActivity extends Activity {
	private final WellbeingStateClient client = new WellbeingStateClient(this);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		startForegroundService(new Intent(MainActivity.this, WellbeingStateHost.class));

		client.doBindService(boundService -> {
			setContentView(R.layout.activity_main);
			PackageManagerDelegate delegate = new PackageManagerDelegate(getPackageManager());
			findViewById(R.id.button).setOnClickListener(a -> {
				boundService.state.reasonMap.put("org.lineageos.jelly", GlobalWellbeingState.REASON.REASON_FOCUS_MODE);
				boundService.state.reasonMap.put("org.lineageos.eleven", GlobalWellbeingState.REASON.REASON_MANUALLY);
				for (String s : delegate.setPackagesSuspended(new String[]{"org.lineageos.jelly","org.lineageos.eleven"},true,null,null,new SuspendDialogInfo.Builder().setTitle(R.string.dialog_title).setMessage(R.string.dialog_message).setNeutralButtonText(R.string.dialog_btn_settings).setNeutralButtonAction(SuspendDialogInfo.BUTTON_ACTION_MORE_DETAILS).setIcon(R.drawable.ic_baseline_app_blocking_24).build())) {
					Log.e("OpenWellbeing", "failed: " + s);
				}
			});
			findViewById(R.id.button2).setOnClickListener(a -> {
				boundService.state.reasonMap.remove("org.lineageos.jelly");
				boundService.state.reasonMap.remove("org.lineageos.eleven");
				for (String s : delegate.setPackagesSuspended(new String[]{"org.lineageos.jelly","org.lineageos.eleven"},false,null,null,null)) {
					Log.e("OpenWellbeing", "failed: " + s);
				}
			});
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		client.doUnbindService();
	}
}