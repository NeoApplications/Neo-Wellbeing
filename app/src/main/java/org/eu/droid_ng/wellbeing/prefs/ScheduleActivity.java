package org.eu.droid_ng.wellbeing.prefs;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.ChargerTriggerCondition;
import org.eu.droid_ng.wellbeing.lib.TimeTriggerCondition;
import org.eu.droid_ng.wellbeing.lib.TriggerCondition;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

public class ScheduleActivity extends AppCompatActivity {

	RadioButton disable, sched, schedc;
	TimeSettingView startTime, endTime;
	DayPicker daypicker;
	String type;
	int checked;

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
		disable = findViewById(R.id.radioButtonDisable);
		sched = findViewById(R.id.radioSchedule);
		schedc = findViewById(R.id.radioCharging);
		startTime = findViewById(R.id.startTime);
		endTime = findViewById(R.id.endTime);
		daypicker = findViewById(R.id.dayPicker);
		findViewById(R.id.layoutDisable).setOnClickListener(v -> setChecked(0));
		findViewById(R.id.layoutSchedule).setOnClickListener(v -> setChecked(1));
		findViewById(R.id.layoutCharging).setOnClickListener(v -> setChecked(2));

		startTime.setData(LocalTime.of(7, 0));
		endTime.setData(LocalTime.of(18, 0));
		daypicker.setValues(new boolean[] { true, true, true, true, true, true, true });
		startTime.setExtraText(getString(R.string.startTime));
		endTime.setExtraText(getString(R.string.endTime));
		startTime.setOnTimeChangedListener(t -> updateServiceStatus());
		endTime.setOnTimeChangedListener(t -> updateServiceStatus());
		daypicker.setOnValuesChangeListener(values -> updateServiceStatus());

		WellbeingService tw = WellbeingService.get();
		List<TriggerCondition> ta = tw.getTriggerConditionForId(type);
		int c = 0;
		TimeTriggerCondition ttc = ta.stream().filter(item -> item instanceof TimeTriggerCondition).map(item -> (TimeTriggerCondition) item).findAny().orElse(null);
		if (ttc != null) {
			startTime.setData(LocalTime.of(ttc.getStartHour(), ttc.getStartMinute()));
			endTime.setData(LocalTime.of(ttc.getEndHour(), ttc.getEndMinute()));
			daypicker.setValues(ttc.getWeekdays());
			if (ta.stream().anyMatch(item -> item instanceof ChargerTriggerCondition)) {
				c = 2;
			} else {
				c = 1;
			}
		}

		setChecked(c);
	}

	private void setChecked(int c) {
		checked = c;
		disable.setChecked(c == 0);
		sched.setChecked(c == 1);
		schedc.setChecked(c == 2);
		startTime.setVisibility(c != 0 ? View.VISIBLE : View.GONE);
		endTime.setVisibility(c != 0 ? View.VISIBLE : View.GONE);
		daypicker.setVisibility(c != 0 ? View.VISIBLE : View.GONE);

		updateServiceStatus();
	}

	private void updateServiceStatus() {
		WellbeingService tw = WellbeingService.get();

		switch (checked) {
			case 1:
				tw.setTriggerConditionForId(type, new TriggerCondition[] { new TimeTriggerCondition(type, startTime.getData().getHour(), startTime.getData().getMinute(), endTime.getData().getHour(), endTime.getData().getMinute(), daypicker.getValues()) });
				break;
			case 2:
				tw.setTriggerConditionForId(type, new TriggerCondition[] { new ChargerTriggerCondition(type), new TimeTriggerCondition(type, startTime.getData().getHour(), startTime.getData().getMinute(), endTime.getData().getHour(), endTime.getData().getMinute(), daypicker.getValues()) });
				break;
			case 0:
			default:
				tw.setTriggerConditionForId(type, new TriggerCondition[] {});
				break;
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}