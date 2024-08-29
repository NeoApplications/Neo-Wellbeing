package org.eu.droid_ng.wellbeing.prefs

import android.app.TimePickerDialog
import android.content.Context
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TimePicker
import androidx.appcompat.widget.AppCompatTextView
import java.time.LocalTime
import java.util.Locale
import java.util.function.Consumer

class TimeSettingView : AppCompatTextView {
	constructor(context: Context?) : super(context!!) {
		initView()
	}

	constructor(context: Context?, attrs: AttributeSet?) : super(
		context!!, attrs
	) {
		initView()
	}

	constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
		context!!, attrs, defStyleAttr
	) {
		initView()
	}

	private var data: LocalTime = LocalTime.of(0, 0)
	private var extraText = ""
	private val use24h = DateFormat.is24HourFormat(context)
	private var onTimeChangedListener: Consumer<LocalTime>? = null

	private fun initView() {
		updateText()
		setOnClickListener { v: View? ->
			TimePickerDialog(context, { tp: TimePicker?, h: Int, m: Int ->
				data = LocalTime.of(h, m)
				updateText()
				if (onTimeChangedListener != null) {
					onTimeChangedListener!!.accept(data)
				}
			}, data.hour, data.minute, use24h).show()
		}
	}

	private fun updateText() {
		var hour: Int
		val minute: Int
		val amPmSymbol: String
		val o = extraText.length + 1
		if (use24h) {
			hour = data.hour
			minute = data.minute
			amPmSymbol = ""
		} else {
			hour = data.hour % 12
			if (hour == 0) {
				hour = 12
			}
			minute = data.minute
			amPmSymbol = " " + (if (data.hour < 12) "AM" else "PM")
		}
		val s = extraText + " " + String.format(Locale.ROOT, "%02d", hour) + ":" + String.format(
			Locale.ROOT, "%02d", minute
		) + amPmSymbol
		val spannableString = SpannableString(s)
		spannableString.setSpan(RelativeSizeSpan(1.5f), o, o + 2, 0) // hour
		spannableString.setSpan(RelativeSizeSpan(1.5f), o + 3, o + 5, 0) // minute
		text = spannableString
	}

	fun setExtraText(extraText: String) {
		this.extraText = extraText
		updateText()
	}

	fun setData(data: LocalTime) {
		this.data = data
		updateText()
	}

	fun getData(): LocalTime {
		return data
	}

	fun setOnTimeChangedListener(onTimeChangedListener: Consumer<LocalTime>?) {
		this.onTimeChangedListener = onTimeChangedListener
	}
}
