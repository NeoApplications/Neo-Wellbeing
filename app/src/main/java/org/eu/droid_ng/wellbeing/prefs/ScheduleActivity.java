package org.eu.droid_ng.wellbeing.prefs;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import org.eu.droid_ng.wellbeing.R;

public class ScheduleActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent() == null || !getIntent().hasExtra("type")) {
			finish();
			return;
		}
		String type = getIntent().getStringExtra("type");
		setContentView(R.layout.activity_schedule);
	}
}