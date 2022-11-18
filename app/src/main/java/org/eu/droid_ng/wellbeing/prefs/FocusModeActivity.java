package org.eu.droid_ng.wellbeing.prefs;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.State;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.util.function.Consumer;

public class FocusModeActivity extends AppCompatActivity {
	private final Consumer<WellbeingService> sc = service -> updateUi();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_focusmode);
		setSupportActionBar(findViewById(R.id.topbar));
		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);
		LayoutTransition layoutTransition = ((LinearLayoutCompat) findViewById(R.id.focusModeRoot)).getLayoutTransition();
		layoutTransition.enableTransitionType(LayoutTransition.CHANGING);

		findViewById(R.id.schedule).setOnClickListener(v ->
						startActivity(new Intent(this, ScheduleActivity.class).putExtra("type", "focus_mode").putExtra("name", getString(R.string.focus_mode))));

		WellbeingService tw = WellbeingService.get();
		tw.addStateCallback(sc);

		RecyclerView r = findViewById(R.id.focusModePkgs);
		r.setAdapter(
				new PackageRecyclerViewAdapter(this,
						tw.getInstalledApplications(PackageManager.GET_META_DATA),
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
		MaterialSwitch toggle = findViewById(R.id.topsw);
		toggle.setChecked(state.isFocusModeEnabled());
		findViewById(R.id.topsc).setOnClickListener(v -> {
			if (state.isFocusModeEnabled()) {
				tw.disableFocusMode();
			} else {
				tw.enableFocusMode();
			}
		});
		View takeBreak = findViewById(R.id.takeBreak);
		((AppCompatTextView) findViewById(R.id.title)).setText(state.isOnFocusModeBreakGlobal() ? R.string.focus_mode_break_end : R.string.focus_mode_break);
		takeBreak.setOnClickListener(v -> {
			if (state.isOnFocusModeBreakGlobal()) {
				tw.endFocusModeBreak();
			} else {
				tw.takeFocusModeBreakWithDialog(FocusModeActivity.this, false, null);
			}
		});
		takeBreak.setVisibility(state.isFocusModeEnabled() ? View.VISIBLE : View.GONE);
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}