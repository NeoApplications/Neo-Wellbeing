package org.eu.droid_ng.wellbeing;

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
	private WellbeingStateClient client;
	private Runnable next;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		client = new WellbeingStateClient(this);
		setContentView(R.layout.activity_main);
		AtomicBoolean focusMode = new AtomicBoolean(false);
		AtomicBoolean focusModeBreak = new AtomicBoolean(false);
		LayoutTransition layoutTransition = ((LinearLayout) findViewById(R.id.focusModeRoot)).getLayoutTransition();
		layoutTransition.enableTransitionType(LayoutTransition.CHANGING);
		next = () -> {
			Button toggle = findViewById(R.id.focusModeToggle);
			TextView takeBreak = findViewById(R.id.focusModeBreak);
			if (!focusMode.get()) {
				toggle.setText(R.string.enable);
				toggle.setOnClickListener(v -> client.doBindService(boundService -> {
					boundService.state.stateChangeCallbacks.add(() -> {
						focusMode.set(boundService.state.type == GlobalWellbeingState.SERVICE_TYPE.TYPE_FOCUS_MODE);
						focusModeBreak.set(boundService.state.focusModeBreak);
						next.run();
					});
					boundService.state.enableFocusMode();
				}, false, true, false));
				takeBreak.setVisibility(View.GONE);
				takeBreak.setOnClickListener(null);
			} else {
				toggle.setText(R.string.disable);
				toggle.setOnClickListener(v -> client.doBindService(boundService -> boundService.state.disableFocusMode()));
				takeBreak.setVisibility(View.VISIBLE);
				if (focusModeBreak.get()) {
					takeBreak.setOnClickListener(v -> client.doBindService(boundService -> boundService.state.endBreak()));
					takeBreak.setText(R.string.focus_mode_break_end);
				} else {
					takeBreak.setOnClickListener(v -> client.doBindService(boundService -> boundService.state.takeBreakDialog(MainActivity.this, false)));
					takeBreak.setText(R.string.focus_mode_break);
				}
			}
		};
		if (!client.isServiceRunning()) {
			focusMode.set(false);
			focusModeBreak.set(false);
			next.run();
		} else {
			client.doBindService(boundService -> {
				Runnable r = () -> {
					focusMode.set(boundService.state.type == GlobalWellbeingState.SERVICE_TYPE.TYPE_FOCUS_MODE);
					focusModeBreak.set(boundService.state.focusModeBreak);
					next.run();
				};
				boundService.state.stateChangeCallbacks.add(r);
				r.run();
			});
		}

		RecyclerView r = findViewById(R.id.focusModePkgs);
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