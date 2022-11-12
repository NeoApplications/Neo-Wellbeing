package org.eu.droid_ng.wellbeing.prefs;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ScheduleActivity extends AppCompatActivity {

	String type;
	HashMap<TimeTriggerCondition, Boolean> data = new HashMap<>();
	LinearLayout cardHost;
	View noCardNotification;

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
		noCardNotification = findViewById(R.id.noCardNotification);

		WellbeingService tw = WellbeingService.get();
		List<TriggerCondition> ta = tw.getTriggerConditionForId(type);
		ta.stream().filter(item -> item instanceof TimeTriggerCondition).map(item -> (TimeTriggerCondition) item).findAny().ifPresent(ttc ->
				data.put(ttc, ta.stream().anyMatch(item -> item instanceof ChargerTriggerCondition)));
		updateUi();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.schedule_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.addSchedule) {
			if (data.keySet().size() == 0) {
				data.put(new TimeTriggerCondition(type, 7, 0, 18, 0, new boolean[]{true, true, true, true, true, true, true}), false);
				updateUi();
			} else {
				Toast.makeText(this, R.string.limit_reached, Toast.LENGTH_LONG).show();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateUi() {
		cardHost.removeAllViews();
		ArrayList<Map.Entry<TimeTriggerCondition, Boolean>> l = new ArrayList<>(data.entrySet());

		if (l.size() < 1) {
			cardHost.setVisibility(View.GONE);
			noCardNotification.setVisibility(View.VISIBLE);
		} else {
			for (Map.Entry<TimeTriggerCondition, Boolean> e : l) {
				ScheduleCardView scv = new ScheduleCardView(this);
				scv.setId(type);
				scv.setTimeData(e.getKey());
				scv.setCharger(e.getValue());
				scv.setOnValuesChangedCallback(() -> {
					data.keySet().stream().filter(v -> v.getId().equals(type)).findFirst().ifPresent(v -> data.remove(v));
					data.put(scv.getTimeData(), scv.isCharger());
					updateUi(); updateServiceStatus();
				});
				scv.setOnDeleteCardCallback(() -> {
					data.keySet().stream().filter(v -> v.getId().equals(type)).findFirst().ifPresent(v -> data.remove(v));
					updateUi(); updateServiceStatus();
				});
				cardHost.addView(scv);
			}
			cardHost.setVisibility(View.VISIBLE);
			noCardNotification.setVisibility(View.GONE);
		}
	}

	private void updateServiceStatus() {
		WellbeingService tw = WellbeingService.get();

		ArrayList<Map.Entry<TimeTriggerCondition, Boolean>> l = new ArrayList<>(data.entrySet());
		if (l.size() < 1) {
			tw.setTriggerConditionForId(type, new TriggerCondition[]{});
		} else {
			Map.Entry<TimeTriggerCondition, Boolean> e = l.get(0);
			if (e.getValue()) {
				tw.setTriggerConditionForId(type, new TriggerCondition[]{ e.getKey(), new ChargerTriggerCondition(type) });
			} else {
				tw.setTriggerConditionForId(type, new TriggerCondition[]{ e.getKey() });
			}
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}