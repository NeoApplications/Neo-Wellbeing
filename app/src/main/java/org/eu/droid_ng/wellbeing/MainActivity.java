package org.eu.droid_ng.wellbeing;

import android.app.Activity;
import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;

import java.util.function.Consumer;

public class MainActivity extends Activity {
	private WellbeingStateClient client;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		client = new WellbeingStateClient(this);
		setContentView(R.layout.activity_main);
		findViewById(R.id.button).setOnClickListener(a ->
				client.doBindService(boundService ->
						boundService.state.manualSuspend(new String[] { "org.lineageos.jelly", "org.lineageos.etar" })
				, false, true, false));
		findViewById(R.id.button2).setOnClickListener(a -> {
			// If service is alive, use it to unsuspend all apps and kill it. If not, start it, unsuspend all apps and kill it again. Avoid a notification with lateNotify = true.
			client.doBindService(boundService -> {
				boundService.state.manualUnsuspend(new String[] { "org.lineageos.jelly", "org.lineageos.etar" });
				client.killService();
			}, false, true, true);
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		client.doUnbindService();
	}
}