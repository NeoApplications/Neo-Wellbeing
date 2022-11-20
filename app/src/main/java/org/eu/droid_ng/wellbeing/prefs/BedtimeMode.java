package org.eu.droid_ng.wellbeing.prefs;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.util.function.Consumer;

public class BedtimeMode extends AppCompatActivity {

	private final Consumer<WellbeingService> sc = tw -> {
		MaterialSwitch bt = findViewById(R.id.topsw);
		bt.setChecked(tw.getState(false).isBedtimeModeEnabled());
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_bedtime_mode);
		setSupportActionBar(findViewById(R.id.topbar));
		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);
		WellbeingService tw = WellbeingService.get();
		SharedPreferences prefs = getSharedPreferences("bedtime_mode", 0);

		MaterialSwitch bt = findViewById(R.id.topsw);
		findViewById(R.id.topsc).setOnClickListener(v -> {
			boolean b = !tw.getState(false).isBedtimeModeEnabled();
			tw.setBedtimeMode(b);
			bt.setChecked(b);
		});
		bt.setChecked(tw.getState(false).isBedtimeModeEnabled());
		MaterialCheckBox checkBox2 = findViewById(R.id.checkBox2);
		checkBox2.setChecked(prefs.getBoolean("greyscale", false));
		findViewById(R.id.greyscaleCheckbox).setOnClickListener(v -> {
			boolean b = !prefs.getBoolean("greyscale", false);
			checkBox2.setChecked(b);
			boolean g = tw.getState(false).isBedtimeModeEnabled();
			prefs.edit().putBoolean("greyscale", b).apply();
			if (g) {
				tw.getCdm().setSaturationLevel(b ? 0 : 100);
			}
		});
		MaterialCheckBox checkBox3 = findViewById(R.id.checkBox3);
		checkBox3.setChecked(prefs.getBoolean("airplane_mode", false));
		findViewById(R.id.airplaneModeCheckbox).setOnClickListener(v -> {
			boolean b = !prefs.getBoolean("airplane_mode", false);
			checkBox3.setChecked(b);
			boolean g = tw.getState(false).isBedtimeModeEnabled();
			prefs.edit().putBoolean("airplane_mode", b).apply();
			if (g) {
				tw.setWellbeingAirplaneMode(b);
			}
		});
		findViewById(R.id.schedule).setOnClickListener(v ->
				startActivity(new Intent(this, ScheduleActivity.class).putExtra("type", "bedtime_mode")
						.putExtra("name", getString(R.string.bedtime_mode))));

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