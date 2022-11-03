package org.eu.droid_ng.wellbeing.prefs;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Toast;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate;

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
		//TODO: implement proper bed time mode
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