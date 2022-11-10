package org.eu.droid_ng.wellbeing.prefs;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate;

import java.util.function.Consumer;

public class BedtimeMode extends AppCompatActivity {

	private final Consumer<WellbeingService> sc = tw -> {
		Button bt = findViewById(R.id.bedModeToggle);
		bt.setText(tw.getState(false).isBedtimeModeEnabled() ? R.string.disable : R.string.enable);
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		setContentView(R.layout.activity_bedtime_mode);
		WellbeingService tw = WellbeingService.get();
		SharedPreferences prefs = getSharedPreferences("bedtime_mode", 0);

		CheckBox c = findViewById(R.id.checkBox2);
		c.setChecked(prefs.getBoolean("greyscale", false));
		Button bt = findViewById(R.id.bedModeToggle);
		bt.setOnClickListener(v -> {
			boolean b = !tw.getState(false).isBedtimeModeEnabled();
			tw.setBedtimeMode(b);
			bt.setText(b ? R.string.disable : R.string.enable);
		});
		bt.setText(tw.getState(false).isBedtimeModeEnabled() ? R.string.disable : R.string.enable);
		findViewById(R.id.greyscaleCheckbox).setOnClickListener(v -> {
			boolean b = !prefs.getBoolean("greyscale", false);
			c.setChecked(b);
			boolean g = tw.getState(false).isBedtimeModeEnabled();
			prefs.edit().putBoolean("greyscale", b).apply();
			if (g) {
				tw.getCdm().setSaturationLevel(b ? 0 : 100);
			}
		});
		TextView schedule = findViewById(R.id.schedule1);
		schedule.setOnClickListener(v -> {
			startActivity(new Intent(this, ScheduleActivity.class).putExtra("type", "bedtime_mode").putExtra("name", getString(R.string.bedtime_mode)));
		});

		//TODO: do not disturb
		//TODO: disable AOD(A11)
		//TODO: dim the wallpaper(A13)
		//TODO: dark theme(A13)

		tw.addStateCallback(sc);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		WellbeingService tw = WellbeingService.get();
		tw.removeStateCallback(sc);
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}