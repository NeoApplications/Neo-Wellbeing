package org.eu.droid_ng.wellbeing.prefs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatCheckBox;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.TimeChargerTriggerCondition;

import java.time.LocalTime;
import java.util.function.Consumer;

public class ScheduleCardView extends FrameLayout {
	public ScheduleCardView(@NonNull Context context) {
		super(context);
		initView();
	}

	public ScheduleCardView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public ScheduleCardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView();
	}

	TimeSettingView startTime, endTime;
	DayPicker daypicker;
	AppCompatCheckBox charger, alarm;
	Consumer<String> onValuesChangedCallback;
	Consumer<String> onDeleteCardCallback;
	String id, iid;

	private void initView() {
		inflate(getContext(), R.layout.schedule_card, this);

		startTime = findViewById(R.id.startTime);
		endTime = findViewById(R.id.endTime);
		daypicker = findViewById(R.id.dayPicker);
		charger = findViewById(R.id.chargerCheckBox);
		alarm = findViewById(R.id.alarmCheckBox);

		daypicker.setValues(new boolean[] { true, true, true, true, true, true, true });
		startTime.setData(LocalTime.of(7, 0));
		endTime.setData(LocalTime.of(18, 0));
		startTime.setExtraText(getContext().getString(R.string.startTime));
		endTime.setExtraText(getContext().getString(R.string.endTime));
		startTime.setOnTimeChangedListener(t -> {
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.accept(iid);
			}
		});
		endTime.setOnTimeChangedListener(t -> {
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.accept(iid);
			}
		});
		daypicker.setOnValuesChangeListener(values -> {
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.accept(iid);
			}
		});
		findViewById(R.id.chargerLayout).setOnClickListener(v -> {
			charger.setChecked(!charger.isChecked());
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.accept(iid);
			}
		});
		findViewById(R.id.alarmLayout).setOnClickListener(v -> {
			alarm.setChecked(!alarm.isChecked());
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.accept(iid);
			}
		});
		findViewById(R.id.delete).setOnClickListener(v -> {
			if (onDeleteCardCallback != null) {
				onDeleteCardCallback.accept(iid);
			}
		});
	}

	public void setOnValuesChangedCallback(Consumer<String> onValuesChangedCallback) {
		this.onValuesChangedCallback = onValuesChangedCallback;
	}

	public void setOnDeleteCardCallback(Consumer<String> onDeleteCardCallback) {
		this.onDeleteCardCallback = onDeleteCardCallback;
	}

	public TimeChargerTriggerCondition getTimeData() {
		LocalTime s = startTime.getData();
		LocalTime e = endTime.getData();
		return new TimeChargerTriggerCondition(id, iid, s.getHour(), s.getMinute(), e.getHour(), e.getMinute(), daypicker.getValues(), charger.isChecked(), alarm.isChecked());
	}

	public void setTimeData(TimeChargerTriggerCondition t) {
		id = t.getId();
		iid = t.getIid();
		startTime.setData(LocalTime.of(t.getStartHour(), t.getStartMinute()));
		endTime.setData(LocalTime.of(t.getEndHour(), t.getEndMinute()));
		daypicker.setValues(t.getWeekdays());
		charger.setChecked(t.getNeedCharger());
		alarm.setChecked(t.getEndOnAlarm());
	}
}
