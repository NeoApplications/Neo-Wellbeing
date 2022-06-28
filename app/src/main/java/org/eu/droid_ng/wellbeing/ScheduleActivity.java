package org.eu.droid_ng.wellbeing;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

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