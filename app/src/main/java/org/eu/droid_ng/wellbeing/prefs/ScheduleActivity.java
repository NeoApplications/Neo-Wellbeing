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
	TextView startTime, endTime;
	String type;
	boolean use24h;
	int checked;
	int sh = 7, sm = 0, eh = 18, em = 0;

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
		use24h = DateFormat.is24HourFormat(this);
		if (intent.hasExtra("defaultStartHour")) {
			sh = intent.getIntExtra("defaultStartHour", sh);
		}
		if (intent.hasExtra("defaultStartMinute")) {
			sm = intent.getIntExtra("defaultStartMinute", sm);
		}
		if (intent.hasExtra("defaultEndHour")) {
			eh = intent.getIntExtra("defaultEndHour", eh);
		}
		if (intent.hasExtra("defaultStartHour")) {
			em = intent.getIntExtra("defaultEndMinute", em);
		}
		if (intent.hasExtra("name")) {
			setTitle(intent.getStringExtra("name"));
		}
		setContentView(R.layout.activity_schedule);
		disable = findViewById(R.id.radioButtonDisable);
		sched = findViewById(R.id.radioSchedule);
		schedc = findViewById(R.id.radioCharging);
		startTime = findViewById(R.id.textView3);
		endTime = findViewById(R.id.textView4);
		findViewById(R.id.layoutDisable).setOnClickListener(v -> setChecked(0));
		findViewById(R.id.layoutSchedule).setOnClickListener(v -> setChecked(1));
		findViewById(R.id.layoutCharging).setOnClickListener(v -> setChecked(2));

		WellbeingService tw = WellbeingService.get();
		List<TriggerCondition> ta = tw.getTriggerConditionForId(type);
		int c = 0;
		TimeTriggerCondition ttc = ta.stream().filter(item -> item instanceof TimeTriggerCondition).map(item -> (TimeTriggerCondition) item).findAny().orElse(null);
		if (ttc != null) {
			sh = ttc.getStartHour();
			sm = ttc.getStartMinute();
			eh = ttc.getEndHour();
			em = ttc.getEndMinute();
			if (ta.stream().anyMatch(item -> item instanceof ChargerTriggerCondition)) {
				c = 2;
			} else {
				c = 1;
			}
		}
		checked = c;
		disable.setChecked(c == 0);
		sched.setChecked(c == 1);
		schedc.setChecked(c == 2);

		setClockTextViewTime(getString(R.string.startTime), startTime, LocalTime.of(sh, sm));
		setClockTextViewTime(getString(R.string.endTime), endTime, LocalTime.of(eh, em));
		startTime.setOnClickListener(v -> onClickClockTextViewDialog(false));
		endTime.setOnClickListener(v -> onClickClockTextViewDialog(true));
		startTime.setVisibility(c != 0 ? View.VISIBLE : View.GONE);
		endTime.setVisibility(c != 0 ? View.VISIBLE : View.GONE);
	}

	private void setChecked(int c) {
		checked = c;
		disable.setChecked(c == 0);
		sched.setChecked(c == 1);
		schedc.setChecked(c == 2);
		startTime.setVisibility(c != 0 ? View.VISIBLE : View.GONE);
		endTime.setVisibility(c != 0 ? View.VISIBLE : View.GONE);

		WellbeingService tw = WellbeingService.get();

		switch (c) {
			case 1:
				tw.setTriggerConditionForId(type, new TriggerCondition[] { new TimeTriggerCondition(type, sh, sm, eh, em) });
				break;
			case 2:
				tw.setTriggerConditionForId(type, new TriggerCondition[] { new ChargerTriggerCondition(type), new TimeTriggerCondition(type, sh, sm, eh, em) });
				break;
			case 0:
			default:
				tw.setTriggerConditionForId(type, new TriggerCondition[] {});
				break;
		}
	}

	private void setClockTextViewTime(String extraText, TextView t, LocalTime data) {
		int hour, minute;
		String amPmSymbol;
		int o = extraText.length() + 1;
		if (use24h) {
			hour = data.getHour();
			minute = data.getMinute();
			amPmSymbol = "";
		} else {
			hour = data.getHour() % 12;
			minute = data.getMinute();
			amPmSymbol = " " + (data.getHour() / 12 == 0 ? "AM" : "PM");
		}
		String s = extraText + " " + String.format(Locale.ROOT, "%02d", hour) + ":" + String.format(Locale.ROOT, "%02d", minute) + amPmSymbol;
		SpannableString spannableString = new SpannableString(s);
		spannableString.setSpan(new RelativeSizeSpan(1.5f), o, o + 2, 0); // hour
		spannableString.setSpan(new RelativeSizeSpan(1.5f), o + 3, o + 5, 0); // minute
		t.setText(spannableString);
	}

	private void onClickClockTextViewDialog(boolean isEnd) {
		TextView v = isEnd ? endTime : startTime;
		String str = getString(isEnd ? R.string.endTime : R.string.startTime);
		new TimePickerDialog(this, (tp, h, m) -> {
			if (isEnd) {
				eh = h;
				em = m;
			} else {
				sh = h;
				sm = m;
			}
			setChecked(checked); // give new hour/min values to service
			setClockTextViewTime(str, v, LocalTime.of(h, m));
		}, isEnd ? eh : sh, isEnd ? em : sm, use24h).show();
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}