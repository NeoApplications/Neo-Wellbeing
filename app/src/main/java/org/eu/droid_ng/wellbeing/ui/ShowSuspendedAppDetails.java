package org.eu.droid_ng.wellbeing.ui;

import static org.eu.droid_ng.wellbeing.lib.BugUtils.BUG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;

import com.google.android.material.card.MaterialCardView;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.State;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate;

public class ShowSuspendedAppDetails extends AppCompatActivity {
	private WellbeingService tw;
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
		tw = WellbeingService.get();
		PackageManager pm = getPackageManager();
		pmd = new PackageManagerDelegate(pm);

		setContentView(R.layout.activity_show_suspended_app_details);
		setSupportActionBar(findViewById(R.id.topbar));
		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);

		final AppCompatImageView iconView = findViewById(R.id.appIcon);
		final AppCompatTextView nameView = findViewById(R.id.appName);
		ApplicationInfo appInfo = null;
		Drawable icon = null;
		CharSequence name = null;
		try {
			appInfo = tw.getApplicationInfo(packageName, false);
			icon = pm.getApplicationIcon(appInfo);
			name = pm.getApplicationLabel(appInfo);
		} catch (PackageManager.NameNotFoundException ignored) {}
		if (appInfo != null && icon != null && name != null) {
			iconView.setImageDrawable(icon);
			nameView.setText(name);
		}
		final State reason = tw.getAppState(packageName);
		MaterialCardView container;
		int hasReason = 0;
		if (reason.isAppTimerExpired() && !reason.isAppTimerBreak()) {
			hasReason++;
			container = findViewById(R.id.apptimer);
			findViewById(R.id.takeabreakbtn2).setOnClickListener(v ->
					tw.takeAppTimerBreakWithDialog(ShowSuspendedAppDetails.this, true, new String[] {packageName}));
			container.setVisibility(View.VISIBLE);
		}
		if (reason.isFocusModeEnabled() && !(reason.isOnFocusModeBreakGlobal() || reason.isOnFocusModeBreakPartial())) {
			hasReason++;
			container = findViewById(R.id.focusMode);
			findViewById(R.id.takeabreakbtn).setOnClickListener(v -> tw.takeFocusModeBreakWithDialog(ShowSuspendedAppDetails.this, true, tw.focusModeAllApps ? null : new String[]{packageName}));
			findViewById(R.id.disablefocusmode).setOnClickListener(v -> {
				tw.disableFocusMode();
				ShowSuspendedAppDetails.this.finish();
			});
			container.setVisibility(View.VISIBLE);
		}
		if (reason.isSuspendedManually()) {
			hasReason++;
			container = findViewById(R.id.manually);
			findViewById(R.id.unsuspendbtn2).setOnClickListener(v -> {
				tw.manualUnsuspend(new String[]{packageName});
				ShowSuspendedAppDetails.this.finish();
			});
			findViewById(R.id.unsuspendallbtn).setOnClickListener(v -> {
				tw.manualUnsuspend(null);
				ShowSuspendedAppDetails.this.finish();
			});
			container.setVisibility(View.VISIBLE);
		}
		if (hasReason < 1 || reason.hasUpdateFailed()) {
			container = findViewById(R.id.unknown);
			findViewById(R.id.unsuspendbtn).setOnClickListener(v -> {
				BUG("Used unknown unsuspend!!");
				pmd.setPackagesSuspended(new String[]{packageName}, false, null, null, null);
				ShowSuspendedAppDetails.this.finish();
			});
			container.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}