package org.eu.droid_ng.wellbeing.prefs

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatCheckBox
import com.google.android.material.materialswitch.MaterialSwitch
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.TimeChargerTriggerCondition
import java.time.LocalTime
import java.util.function.Consumer

class ScheduleCardView(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
	: FrameLayout(context, attrs, defStyleAttr) {
	constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
	constructor(context: Context) : this(context, null)

	private var startTime: TimeSettingView
	private var endTime: TimeSettingView
	private var daypicker: DayPicker
	private var enable: MaterialSwitch
	private var charger: AppCompatCheckBox
	private var alarm: AppCompatCheckBox
	var onValuesChangedCallback: Consumer<String?>? = null
	var onDeleteCardCallback: Consumer<String?>? = null
	var id: String? = null
	var iid: String? = null

	init {
		inflate(context, R.layout.schedule_card, this)

		startTime = findViewById(R.id.startTime)
		endTime = findViewById(R.id.endTime)
		daypicker = findViewById(R.id.dayPicker)
		enable = findViewById(R.id.enableCheckBox)
		charger = findViewById(R.id.chargerCheckBox)
		alarm = findViewById(R.id.alarmCheckBox)

		daypicker.values = booleanArrayOf(true, true, true, true, true, true, true)
		startTime.setData(LocalTime.of(7, 0))
		endTime.setData(LocalTime.of(18, 0))
		startTime.setOnTimeChangedListener {
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback!!.accept(iid)
			}
		}
		endTime.setOnTimeChangedListener {
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback!!.accept(iid)
			}
		}
		daypicker.setOnValuesChangeListener {
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback!!.accept(iid)
			}
		}
		enable.setOnCheckedChangeListener { _, _ ->
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback!!.accept(iid)
			}
		}
		findViewById<View>(R.id.chargerLayout).setOnClickListener {
			charger.setChecked(!charger.isChecked)
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback!!.accept(iid)
			}
		}
		findViewById<View>(R.id.alarmLayout).setOnClickListener {
			alarm.setChecked(!alarm.isChecked)
			if (onValuesChangedCallback != null) {
				onValuesChangedCallback!!.accept(iid)
			}
		}
		findViewById<View>(R.id.delete).setOnClickListener {
			if (onDeleteCardCallback != null) {
				onDeleteCardCallback!!.accept(iid)
			}
		}
	}

	var timeData: TimeChargerTriggerCondition
		get() {
			val s = startTime.getData()
			val e = endTime.getData()
			return TimeChargerTriggerCondition(
				id!!,
				iid!!,
				enable.isChecked,
				s.hour,
				s.minute,
				e.hour,
				e.minute,
				daypicker.values,
				charger.isChecked,
				alarm.isChecked
			)
		}
		set(t) {
			id = t.id
			iid = t.iid
			enable.isChecked = t.enabled
			startTime.setData(LocalTime.of(t.startHour, t.startMinute))
			endTime.setData(LocalTime.of(t.endHour, t.endMinute))
			daypicker.values = t.weekdays
			charger.isChecked = t.needCharger
			alarm.isChecked = t.endOnAlarm
		}
}
