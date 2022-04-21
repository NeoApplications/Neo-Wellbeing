package org.eu.droid_ng.wellbeing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class ShowSuspendedAppDetails extends Activity {
	public final WellbeingStateClient client = new WellbeingStateClient(this);

	@SuppressLint("SetTextI18n")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		client.doBindService(boundService -> {
			setContentView(R.layout.activity_show_suspended_app_details);
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_close_24);
			String packageName = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
			if (packageName == null) {
				Toast.makeText(ShowSuspendedAppDetails.this, "Assertion failure (0xAB): packageName is null. Please report this to the developers!", Toast.LENGTH_LONG).show();
				finish();
				return;
			}
			TextView t = findViewById(R.id.textView);
			t.setText(packageName + ": " + boundService.state.reasonMap.getOrDefault(packageName, GlobalWellbeingState.REASON.REASON_UNKNOWN));

		});

	}

	@Override
	public boolean onNavigateUp() {
		finish();
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		client.doUnbindService();
	}
}