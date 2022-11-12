package org.eu.droid_ng.wellbeing.prefs;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.ChargerTriggerCondition;
import org.eu.droid_ng.wellbeing.lib.TimeTriggerCondition;
import org.eu.droid_ng.wellbeing.lib.TriggerCondition;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScheduleActivity extends AppCompatActivity {
	String type;
	ViewGroup cardHost;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		Intent intent = getIntent();
		type = null;
		if (intent != null && intent.hasExtra("type")) {
			type = intent.getStringExtra("type");
		}
		if (type == null) {
			Log.e("ScheduleActivity", "intent or type is null");
			finish();
			return;
		}
		if (intent.hasExtra("name")) {
			setTitle(intent.getStringExtra("name"));
		}
		setContentView(R.layout.activity_schedule);
		cardHost = findViewById(R.id.cardHost);

		WellbeingService tw = WellbeingService.get();
		List<TriggerCondition> ta = tw.getTriggerConditionForId(type);
		for (TriggerCondition t : ta) {
			ScheduleCardView scv = new ScheduleCardView(this);
			scv.setId(t.getId());
			cardHost.addView(scv);
			scv.setData(t);
			scv.setOnValuesChangedCallback(tt -> updateServiceState());
			scv.setOnDeleteCardCallback(() -> {
				cardHost.removeView(scv);
				updateServiceState();
			});
		}

		if (cardHost.getChildCount() != 0) {
			findViewById(R.id.textView5).setVisibility(View.GONE);
			cardHost.setVisibility(View.VISIBLE);
		}
	}

	private void updateServiceState() {
		if (cardHost.getChildCount() != 0) {
			findViewById(R.id.textView5).setVisibility(View.GONE);
			cardHost.setVisibility(View.VISIBLE);
		}

		WellbeingService tw = WellbeingService.get();
		ArrayList<TriggerCondition> data = new ArrayList<>();
		for (int i = 0; i < cardHost.getChildCount(); i++) {
			ScheduleCardView scv = (ScheduleCardView) cardHost.getChildAt(i);
			data.add(scv.getData());
		}
		tw.setTriggerConditionForId(type, data.toArray(new TriggerCondition[0]));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.schedule_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.addSchedule) {
			ScheduleCardView scv = new ScheduleCardView(this);
			scv.setId(type + "|" + System.currentTimeMillis());
			cardHost.addView(scv);
			scv.setOnValuesChangedCallback(tt -> updateServiceState());
			scv.setOnDeleteCardCallback(() -> {
				cardHost.removeView(scv);
				updateServiceState();
			});
			updateServiceState();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}