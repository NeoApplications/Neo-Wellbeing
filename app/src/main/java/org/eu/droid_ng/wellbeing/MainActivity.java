package org.eu.droid_ng.wellbeing;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends Activity {
	private WellbeingStateClient client;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		client = new WellbeingStateClient(this);
		setContentView(R.layout.activity_main);
		findViewById(R.id.button).setOnClickListener(a ->
				client.doBindService(boundService ->
						boundService.state.enableFocusMode()
						//boundService.state.manualSuspend(new String[] { "org.lineageos.jelly", "org.lineageos.etar" })
				, false, true, false));
		findViewById(R.id.button2).setOnClickListener(a -> {
			// If service is alive, use it to unsuspend all apps and kill it. If not, start it, unsuspend all apps and kill it again. Avoid a notification with lateNotify = true.
			client.doBindService(boundService ->
					//boundService.state.manualUnsuspend(null)
					boundService.state.disableFocusMode()
			, false, true, true);
		});
		findViewById(R.id.button3).setOnClickListener(a ->
				startActivity(new Intent(MainActivity.this, SettingsActivity.class)));
		RecyclerView r = findViewById(R.id.recyclerView);
		r.setAdapter(
				new PackageRecyclerViewAdapter(this,
						getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA),
						"focus_mode"));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		client.doUnbindService();
	}
}