package org.eu.droid_ng.wellbeing.prefs;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import java.time.LocalTime;
import java.util.Locale;
import java.util.function.Consumer;

public class TimeSettingView extends AppCompatTextView {
	public TimeSettingView(Context context) {
		super(context);
		initView();
	}

	public TimeSettingView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		initView();
	}

	public TimeSettingView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		initView();
	}

	private LocalTime data = LocalTime.of(0, 0);
	private String extraText = "";
	private final boolean use24h = DateFormat.is24HourFormat(getContext());
	private Consumer<LocalTime> onTimeChangedListener = null;

	private void initView() {
		updateText();
		setOnClickListener(v -> new TimePickerDialog(getContext(), (tp, h, m) -> {
			data = LocalTime.of(h, m);
			updateText();
			if (onTimeChangedListener != null) {
				onTimeChangedListener.accept(data);
			}
		}, data.getHour(), data.getMinute(), use24h).show());
	}

	private void updateText() {
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
		setText(spannableString);
	}

	public void setExtraText(String extraText) {
		this.extraText = extraText;
		updateText();
	}

	public void setData(LocalTime data) {
		this.data = data;
		updateText();
	}

	public LocalTime getData() {
		return data;
	}

	public void setOnTimeChangedListener(Consumer<LocalTime> onTimeChangedListener) {
		this.onTimeChangedListener = onTimeChangedListener;
	}
}
