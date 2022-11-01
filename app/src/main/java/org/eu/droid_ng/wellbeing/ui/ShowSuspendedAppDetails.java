package org.eu.droid_ng.wellbeing.ui;

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

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.State;
import org.eu.droid_ng.wellbeing.lib.TransistentWellbeingState;
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate;

public class ShowSuspendedAppDetails extends Activity {
	private TransistentWellbeingState tw;
	private PackageManagerDelegate pmd;

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
		TransistentWellbeingState.use(this, tw -> {
			PackageManager pm = getPackageManager();
			pmd = new PackageManagerDelegate(pm);

			setContentView(R.layout.activity_show_suspended_app_details);
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_close_24);

			final ImageView iconView = findViewById(R.id.appIcon);
			final TextView nameView = findViewById(R.id.appName);
			ApplicationInfo appInfo = null;
			Drawable icon = null;
			CharSequence name = null;
			try {
				appInfo = pm.getApplicationInfo(packageName, 0);
				icon = pm.getApplicationIcon(appInfo);
				name = pm.getApplicationLabel(appInfo);
			} catch (PackageManager.NameNotFoundException ignored) {}
			if (appInfo != null && icon != null && name != null) {
				iconView.setImageDrawable(icon);
				nameView.setText(name);
			}
			final State reason = tw.getAppState(packageName);
			CardView container;
			/*
			if ((reason & TransistentWellbeingState.STATE_SUSPEND_APP_TIMER_EXPIRED) > 0) {
				container = findViewById(R.id.apptimer);
				findViewById(R.id.takeabreakbtn2).setOnClickListener(v ->
						AppTimersInternal.get(this).appTimerSuspendHook(packageName));
				container.setVisibility(View.VISIBLE);
			}
			*/
			if (reason.isFocusModeEnabled()) {
				container = findViewById(R.id.focusMode);
				findViewById(R.id.takeabreakbtn).setOnClickListener(v -> tw.takeFocusModeBreakWithDialog(ShowSuspendedAppDetails.this, true, tw.getBoolSetting("focusModeAllApps") ? null : new String[]{packageName}));
				findViewById(R.id.disablefocusmode).setOnClickListener(v -> {
					tw.disableFocusMode();
					ShowSuspendedAppDetails.this.finish();
				});
				container.setVisibility(View.VISIBLE);
			}
			/*
			if ((reason & TransistentWellbeingState.STATE_SUSPEND_MANUAL) > 0) {
				container = findViewById(R.id.manually);
				findViewById(R.id.unsuspendbtn2).setOnClickListener(v -> {
					tw..manualUnsuspend(new String[]{packageName}, false);
					ShowSuspendedAppDetails.this.finish();
				});
				findViewById(R.id.unsuspendallbtn).setOnClickListener(v -> {
					tw..manualUnsuspend(null, true);
					ShowSuspendedAppDetails.this.finish();
				});
				container.setVisibility(View.VISIBLE);
			}
			if ((reason & TransistentWellbeingState.STATE_SUSPEND_UNKNOWN_REASON) > 0) {
				container = findViewById(R.id.unknown);
				findViewById(R.id.unsuspendbtn).setOnClickListener(v -> {
					pmd.setPackagesSuspended(new String[]{packageName}, false, null, null, null);
					tw.requireState().reasonMap.remove(packageName);
					ShowSuspendedAppDetails.this.finish();
				});
				container.setVisibility(View.VISIBLE);
			}*/
		});
	}

	@Override
	public boolean onNavigateUp() {
		finish();
		return true;
	}
}