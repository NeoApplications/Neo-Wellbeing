package org.eu.droid_ng.wellbeing.prefs;

import static org.eu.droid_ng.wellbeing.lib.BugUtils.BUG;

import android.animation.LayoutTransition;
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
import org.eu.droid_ng.wellbeing.lib.TransistentWellbeingState;

public class FocusModeActivity extends AppCompatActivity {
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
		schedule.setOnClickListener(v -> {
			BUG("Tried to use schedule which is not supported at the moment");
			//startActivity(new Intent(this, ScheduleActivity.class).putExtra("type", "focus_mode"));
		});

		RecyclerView r = findViewById(R.id.focusModePkgs);
		r.setAdapter(
				new PackageRecyclerViewAdapter(this,
						getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA),
						"focus_mode"));

		updateUiAsync();
	}

	private void updateUiAsync() {
		TransistentWellbeingState.use(this, tw -> {
			State state = tw.getState();
			Button toggle = findViewById(R.id.focusModeToggle);
			TextView takeBreak = findViewById(R.id.focusModeBreak);
			if (!state.isFocusModeEnabled()) {
				toggle.setText(R.string.enable);
				toggle.setOnClickListener(v -> tw.later(tw::enableFocusMode));
				takeBreak.setVisibility(View.GONE);
				takeBreak.setOnClickListener(null);
			} else {
				toggle.setText(R.string.disable);
				toggle.setOnClickListener(v -> tw.later(tw::disableFocusMode));
				takeBreak.setVisibility(View.VISIBLE);
				if (state.isOnFocusModeBreakGlobal()) {
					takeBreak.setOnClickListener(v -> tw.later(tw::endFocusModeBreak));
					takeBreak.setText(R.string.focus_mode_break_end);
				} else {
					takeBreak.setOnClickListener(v -> tw.later(() -> tw.takeFocusModeBreakWithDialog(FocusModeActivity.this, false, null)));
					takeBreak.setText(R.string.focus_mode_break);
				}
			}
		});
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}