package org.eu.droid_ng.wellbeing.prefs;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.State;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.util.function.Consumer;

public class FocusModeActivity extends AppCompatActivity {
	private final Consumer<WellbeingService> sc = service -> updateUi();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		setContentView(R.layout.activity_focusmode);
		LayoutTransition layoutTransition = ((LinearLayout) findViewById(R.id.focusModeRoot)).getLayoutTransition();
		layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

		TextView schedule = findViewById(R.id.schedule);
		schedule.setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class).putExtra("type", "focus_mode").putExtra("name", getString(R.string.focus_mode))));

		WellbeingService tw = WellbeingService.get();
		tw.addStateCallback(sc);

		RecyclerView r = findViewById(R.id.focusModePkgs);
		r.setAdapter(
				new PackageRecyclerViewAdapter(this,
						getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA),
						"focus_mode", tw::onFocusModePreferenceChanged));

		updateUi();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		WellbeingService tw = WellbeingService.get();
		tw.removeStateCallback(sc);
	}

	private void updateUi() {
		WellbeingService tw = WellbeingService.get();
		State state = tw.getState();
		Button toggle = findViewById(R.id.focusModeToggle);
		TextView takeBreak = findViewById(R.id.focusModeBreak);
		if (!state.isFocusModeEnabled()) {
			toggle.setText(R.string.enable);
			toggle.setOnClickListener(v -> tw.enableFocusMode());
			takeBreak.setVisibility(View.GONE);
			takeBreak.setOnClickListener(null);
		} else {
			toggle.setText(R.string.disable);
			toggle.setOnClickListener(v -> tw.disableFocusMode());
			takeBreak.setVisibility(View.VISIBLE);
			if (state.isOnFocusModeBreakGlobal()) {
				takeBreak.setOnClickListener(v -> tw.endFocusModeBreak());
				takeBreak.setText(R.string.focus_mode_break_end);
			} else {
				takeBreak.setOnClickListener(v -> tw.takeFocusModeBreakWithDialog(FocusModeActivity.this, false, null));
				takeBreak.setText(R.string.focus_mode_break);
			}
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}