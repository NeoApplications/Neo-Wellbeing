package org.eu.droid_ng.wellbeing;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.eu.droid_ng.wellbeing.lib.GlobalWellbeingState;
import org.eu.droid_ng.wellbeing.lib.WellbeingStateClient;

import java.util.concurrent.atomic.AtomicBoolean;

public class FocusModeActivity extends AppCompatActivity {
	private WellbeingStateClient client;
	private Runnable next;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		client = new WellbeingStateClient(this);
		setContentView(R.layout.activity_focusmode);
		AtomicBoolean focusMode = new AtomicBoolean(false);
		AtomicBoolean focusModeBreak = new AtomicBoolean(false);
		LayoutTransition layoutTransition = ((LinearLayout) findViewById(R.id.focusModeRoot)).getLayoutTransition();
		layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
		next = () -> {
			Button toggle = findViewById(R.id.focusModeToggle);
			TextView takeBreak = findViewById(R.id.focusModeBreak);
			if (!focusMode.get()) {
				toggle.setText(R.string.enable);
				toggle.setOnClickListener(v -> client.doBindService(state -> {
					state.stateChangeCallbacks.add(() -> {
						focusMode.set(state.type == GlobalWellbeingState.SERVICE_TYPE.TYPE_FOCUS_MODE);
						focusModeBreak.set(state.focusModeBreak);
						next.run();
					});
					state.enableFocusMode();
				}, false, true, false));
				takeBreak.setVisibility(View.GONE);
				takeBreak.setOnClickListener(null);
			} else {
				toggle.setText(R.string.disable);
				toggle.setOnClickListener(v -> client.doBindService(GlobalWellbeingState::disableFocusMode, false, true, true));
				takeBreak.setVisibility(View.VISIBLE);
				if (focusModeBreak.get()) {
					takeBreak.setOnClickListener(v -> client.doBindService(GlobalWellbeingState::endBreak));
					takeBreak.setText(R.string.focus_mode_break_end);
				} else {
					takeBreak.setOnClickListener(v -> client.doBindService(state -> state.takeBreakDialog(FocusModeActivity.this, false, null)));
					takeBreak.setText(R.string.focus_mode_break);
				}
			}
		};
		if (!client.isServiceRunning()) {
			focusMode.set(false);
			focusModeBreak.set(false);
			next.run();
		} else {
			client.doBindService(state -> {
				Runnable r = () -> {
					focusMode.set(state.type == GlobalWellbeingState.SERVICE_TYPE.TYPE_FOCUS_MODE);
					focusModeBreak.set(state.focusModeBreak);
					next.run();
				};
				state.stateChangeCallbacks.add(r);
				r.run();
			});
		}

		TextView schedule = findViewById(R.id.schedule);
		schedule.setOnClickListener(v -> {
			startActivity(new Intent(this, ScheduleActivity.class).putExtra("type", "focus_mode"));
		});

		RecyclerView r = findViewById(R.id.focusModePkgs);
		r.setAdapter(
				new PackageRecyclerViewAdapter(this,
						getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA),
						"focus_mode"));
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		client.doUnbindService();
	}
}