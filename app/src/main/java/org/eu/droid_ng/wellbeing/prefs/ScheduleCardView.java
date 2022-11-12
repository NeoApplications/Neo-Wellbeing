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
	Consumer<TriggerCondition> onValuesChangedCallback;
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
				onValuesChangedCallback.accept(getData());
			}
		});
		endTime.setExtraText(getContext().getString(R.string.endTime));
		endTime.setOnTimeChangedListener(t -> {
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.accept(getData());
			}
		});
		daypicker.setOnValuesChangeListener(values -> {
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.accept(getData());
			}
		});
		findViewById(R.id.chargerLayout).setOnClickListener(v -> {
			checkBox.setChecked(charger = !charger);
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback.accept(getData());
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

	public void setOnValuesChangedCallback(Consumer<TriggerCondition> onValuesChangedCallback) {
		this.onValuesChangedCallback = onValuesChangedCallback;
	}

	public void setOnDeleteCardCallback(Runnable onDeleteCardCallback) {
		this.onDeleteCardCallback = onDeleteCardCallback;
	}

	public TriggerCondition getData() {
		LocalTime s = startTime.getData();
		LocalTime e = endTime.getData();
		boolean[] w = daypicker.getValues();
		TimeTriggerCondition t = new TimeTriggerCondition(id, s.getHour(), s.getMinute(), e.getHour(), e.getMinute(), w);
		if (charger) {
			//return new TimeChargerTriggerCondition(id, t, new ChargerTriggerCondition(id));
		}
		return t;
	}

	public void setData(TriggerCondition tt) {
		TimeTriggerCondition t;
		/*if (tt instanceof TimeChargerTriggerCondition) {
			charger = true;
			t = ((TimeChargerTriggerCondition)tt).getTimeTriggerCondition();
		} else {*/
			t = (TimeTriggerCondition)tt;
		//}
		startTime.setData(LocalTime.of(t.getStartHour(), t.getStartMinute()));
		endTime.setData(LocalTime.of(t.getEndHour(), t.getEndMinute()));
		daypicker.setValues(t.getWeekdays());

		checkBox.setChecked(charger);
	}
}
