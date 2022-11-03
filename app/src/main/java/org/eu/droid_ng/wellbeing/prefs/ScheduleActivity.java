package org.eu.droid_ng.wellbeing.prefs;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.RadioButton;

import org.eu.droid_ng.wellbeing.R;

public class ScheduleActivity extends AppCompatActivity {

	RadioButton disable, sched, schedc;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent() == null || !getIntent().hasExtra("type")) {
			finish();
			return;
		}
		String type = getIntent().getStringExtra("type");
		setContentView(R.layout.activity_schedule);
		disable = findViewById(R.id.radioButtonDisable);
		sched = findViewById(R.id.radioSchedule);
		schedc = findViewById(R.id.radioCharging);
		findViewById(R.id.layoutDisable).setOnClickListener(v -> setChecked(0));
		findViewById(R.id.layoutSchedule).setOnClickListener(v -> setChecked(1));
		findViewById(R.id.layoutCharging).setOnClickListener(v -> setChecked(2));
		//TODO: implement schedules
	}

	private void setChecked(int c) {
		disable.setChecked(c == 0);
		sched.setChecked(c == 1);
		schedc.setChecked(c == 2);
	}
}