package org.eu.droid_ng.wellbeing.prefs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.ChargerTriggerCondition;
import org.eu.droid_ng.wellbeing.lib.TimeTriggerCondition;
import org.eu.droid_ng.wellbeing.lib.TriggerCondition;

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
	CheckBox checkBox;
	boolean charger;
	Runnable onValuesChangedCallback;
	Runnable onDeleteCardCallback;
	String id;

	private void initView() {
		inflate(getContext(), R.layout.schedule_card, this);

		startTime = findViewById(R.id.startTime);
		endTime = findViewById(R.id.endTime);
		daypicker = findViewById(R.id.dayPicker);
		checkBox = findViewById(R.id.chargerCheckBox);

		daypicker.setValues(new boolean[] { true, true, true, true, true, true, true });
		startTime.setData(LocalTime.of(7, 0));
		endTime.setData(LocalTime.of(18, 0));
		startTime.setExtraText(getContext().getString(R.string.startTime));
		startTime.setOnTimeChangedListener(t -> {
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.run();
			}
		});
		endTime.setExtraText(getContext().getString(R.string.endTime));
		endTime.setOnTimeChangedListener(t -> {
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.run();
			}
		});
		daypicker.setOnValuesChangeListener(values -> {
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.run();
			}
		});
		findViewById(R.id.chargerLayout).setOnClickListener(v -> {
			setCharger(!charger);
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.run();
			}
		});
		findViewById(R.id.delete).setOnClickListener(v -> {
			if (onDeleteCardCallback != null) {
				onDeleteCardCallback.run();
			}
		});
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setOnValuesChangedCallback(Runnable onValuesChangedCallback) {
		this.onValuesChangedCallback = onValuesChangedCallback;
	}

	public void setOnDeleteCardCallback(Runnable onDeleteCardCallback) {
		this.onDeleteCardCallback = onDeleteCardCallback;
	}

	public TimeTriggerCondition getTimeData() {
		LocalTime s = startTime.getData();
		LocalTime e = endTime.getData();
		boolean[] w = daypicker.getValues();
		return new TimeTriggerCondition(id, s.getHour(), s.getMinute(), e.getHour(), e.getMinute(), w);
	}

	public boolean isCharger() {
		return charger;
	}

	public void setCharger(boolean charger) {
		this.charger = charger;
		checkBox.setChecked(charger);
	}

	public void setTimeData(TimeTriggerCondition t) {
		startTime.setData(LocalTime.of(t.getStartHour(), t.getStartMinute()));
		endTime.setData(LocalTime.of(t.getEndHour(), t.getEndMinute()));
		daypicker.setValues(t.getWeekdays());
	}
}
