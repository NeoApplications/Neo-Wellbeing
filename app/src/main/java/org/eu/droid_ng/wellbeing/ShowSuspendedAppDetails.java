package org.eu.droid_ng.wellbeing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import java.util.concurrent.atomic.AtomicReference;

public class ShowSuspendedAppDetails extends Activity {
	public final WellbeingStateClient client = new WellbeingStateClient(this);

	@SuppressLint("SetTextI18n")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final String packageName = getIntent().getStringExtra(Intent.EXTRA_PACKAGE_NAME);
		if (packageName == null) {
			Toast.makeText(ShowSuspendedAppDetails.this, "Assertion failure (0xAB): packageName is null. Please report this to the developers!", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		setContentView(R.layout.activity_show_suspended_app_details);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_close_24);

		final ImageView iconView = findViewById(R.id.appIcon);
		final TextView nameView = findViewById(R.id.appName);
		ApplicationInfo appInfo = null;
		Drawable icon = null;
		CharSequence name = null;
		try {
			appInfo = getPackageManager().getApplicationInfo(packageName, 0);
			icon = getPackageManager().getApplicationIcon(appInfo);
			name = getPackageManager().getApplicationLabel(appInfo);
		} catch (PackageManager.NameNotFoundException ignored) {
		}
		if (appInfo != null && icon != null && name != null) {
			iconView.setImageDrawable(icon);
			nameView.setText(name);
		}
		final AtomicReference<GlobalWellbeingState.REASON> reason = new AtomicReference<>(GlobalWellbeingState.REASON.REASON_UNKNOWN);
		final AtomicReference<Runnable> reset = new AtomicReference<>(() -> {});
		final Runnable next = () -> {
			final CardView container;
			switch (reason.get()) {
				case REASON_FOCUS_MODE:
					container = findViewById(R.id.focusMode);
					findViewById(R.id.takeabreakbtn).setOnClickListener(v -> {
						client.doBindService(boundService -> {
							boundService.state.takeBreak(1);
						});
					});
					break;
				case REASON_MANUALLY:
					container = findViewById(R.id.manually);
					break;
				case REASON_UNKNOWN:
				default:
					container = findViewById(R.id.unknown);
			}
			container.setVisibility(View.VISIBLE);
		};
		/* If service does not exist, just set UNKNOWN reason. If it exists, use its data */
		boolean hasService = client.doBindService(boundService -> {
			reason.set(boundService.state.reasonMap.getOrDefault(packageName, GlobalWellbeingState.REASON.REASON_UNKNOWN));
			reset.set(() -> boundService.state.reasonMap.remove(packageName));
			next.run();
		}, false);
		if (!hasService)
			next.run();
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