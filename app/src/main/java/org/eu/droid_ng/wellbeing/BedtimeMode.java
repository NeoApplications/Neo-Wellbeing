package org.eu.droid_ng.wellbeing;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BedtimeMode extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		PackageManagerDelegate.IColorDisplayManager cdm = PackageManagerDelegate.getColorDisplayManager(this);
		if (cdm == null) {
			Toast.makeText(this, "ColorDisplayManager is null", Toast.LENGTH_LONG).show();
			return;
		}
		setContentView(R.layout.activity_bedtime_mode);

		findViewById(R.id.button).setOnClickListener(v -> {
			cdm.setSaturationLevel(0);
		});
		findViewById(R.id.button2).setOnClickListener(v -> {
			cdm.setSaturationLevel(100);
		});
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}