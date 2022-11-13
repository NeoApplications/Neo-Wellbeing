package org.eu.droid_ng.wellbeing.prefs;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;

import org.eu.droid_ng.wellbeing.R;

import java.util.Calendar;
import java.util.function.Consumer;

public class DayPicker extends FrameLayout {
	private ToggleButton[] views;
	private boolean[] values = new boolean[7]; // Monday -> Sunday like Java DayOfWeek
	private int firstDayOfWeek;
	private Consumer<boolean[]> onValuesChangeListener;

	public DayPicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		initView();
	}

	public DayPicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public DayPicker(Context context) {
		super(context);
		initView();
	}

	private void initView() {
		inflate(getContext(), R.layout.dpicker, this);
		ToggleButton day1 = findViewById(R.id.dayPickerDay1);
		ToggleButton day2 = findViewById(R.id.dayPickerDay2);
		ToggleButton day3 = findViewById(R.id.dayPickerDay3);
		ToggleButton day4 = findViewById(R.id.dayPickerDay4);
		ToggleButton day5 = findViewById(R.id.dayPickerDay5);
		ToggleButton day6 = findViewById(R.id.dayPickerDay6);
		ToggleButton day7 = findViewById(R.id.dayPickerDay7);
		views = new ToggleButton[] {day1, day2, day3, day4, day5, day6, day7};

		firstDayOfWeek = (Calendar.getInstance().getFirstDayOfWeek() - 2) % 7;
		for (int i = 0; i < 7; i++) {
			ToggleButton v = views[i];
			int textToSet;
			int javaDayOfWeek = Math.floorMod((i + firstDayOfWeek), 7);
			switch (javaDayOfWeek + 2) {
				case Calendar.MONDAY:
					textToSet = R.string.dpicker_monday;
					break;
				case Calendar.TUESDAY:
					textToSet = R.string.dpicker_tuesday;
					break;
				case Calendar.WEDNESDAY:
					textToSet = R.string.dpicker_wednesday;
					break;
				case Calendar.THURSDAY:
					textToSet = R.string.dpicker_thursday;
					break;
				case Calendar.FRIDAY:
					textToSet = R.string.dpicker_friday;
					break;
				case Calendar.SATURDAY:
					textToSet = R.string.dpicker_saturday;
					break;
				case Calendar.SUNDAY:
				default:
					textToSet = R.string.dpicker_sunday;
					break;
			}
			CharSequence textToSet2 = getContext().getString(textToSet);
			v.setTextOn(textToSet2);
			v.setTextOff(textToSet2);
			v.setOnCheckedChangeListener((view, isChecked) -> {
				values[javaDayOfWeek] = isChecked;
				if (onValuesChangeListener != null) {
					onValuesChangeListener.accept(values);
				}
			});
		}

		setValues(values);
	}

	public void setValues(boolean[] values) {
		this.values = values;
		for (int i = 0; i < 7; i++) {
			ToggleButton v = views[i];
			int javaDayOfWeek = Math.floorMod((i + firstDayOfWeek), 7);
			v.setChecked(values[javaDayOfWeek]);
		}
	}

	public boolean[] getValues() {
		return values;
	}

	public void setOnValuesChangeListener(@Nullable Consumer<boolean[]> onValuesChangeListener) {
		this.onValuesChangeListener = onValuesChangeListener;
	}
}