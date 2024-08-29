package org.eu.droid_ng.wellbeing.prefs

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatToggleButton
import org.eu.droid_ng.wellbeing.R
import java.util.Calendar
import java.util.function.Consumer

class DayPicker(context: Context, attrs: AttributeSet?, defStyle: Int)
	: FrameLayout(context, attrs, defStyle) {
	private var views: Array<AppCompatToggleButton>
	var values = BooleanArray(7) // Monday -> Sunday like Java DayOfWeek
		set(value) {
			field = value
			for (i in 0..6) {
				val v = views[i]
				val javaDayOfWeek = Math.floorMod((i + firstDayOfWeek), 7)
				v.isChecked = field[javaDayOfWeek]
			}
		}
	private var firstDayOfWeek = 0
	private var onValuesChangeListener: Consumer<BooleanArray>? = null

	constructor(context: Context, attrs: AttributeSet?) : this(
		context, attrs, 0
	)
	constructor(context: Context) : this(context, null)

	init {
		inflate(context, R.layout.dpicker, this)
		val day1 = findViewById<AppCompatToggleButton>(R.id.dayPickerDay1)
		val day2 = findViewById<AppCompatToggleButton>(R.id.dayPickerDay2)
		val day3 = findViewById<AppCompatToggleButton>(R.id.dayPickerDay3)
		val day4 = findViewById<AppCompatToggleButton>(R.id.dayPickerDay4)
		val day5 = findViewById<AppCompatToggleButton>(R.id.dayPickerDay5)
		val day6 = findViewById<AppCompatToggleButton>(R.id.dayPickerDay6)
		val day7 = findViewById<AppCompatToggleButton>(R.id.dayPickerDay7)
		views = arrayOf(day1, day2, day3, day4, day5, day6, day7)

		firstDayOfWeek = (Calendar.getInstance().firstDayOfWeek - 2) % 7
		for (i in 0..6) {
			val v = views[i]
			var textToSet: Int
			val javaDayOfWeek = Math.floorMod((i + firstDayOfWeek), 7)
			textToSet = when (javaDayOfWeek + 2) {
				Calendar.MONDAY -> R.string.dpicker_monday
				Calendar.TUESDAY -> R.string.dpicker_tuesday
				Calendar.WEDNESDAY -> R.string.dpicker_wednesday
				Calendar.THURSDAY -> R.string.dpicker_thursday
				Calendar.FRIDAY -> R.string.dpicker_friday
				Calendar.SATURDAY -> R.string.dpicker_saturday
				Calendar.SUNDAY -> R.string.dpicker_sunday
				else -> R.string.dpicker_sunday
			}
			val textToSet2: CharSequence = context.getString(textToSet)
			v.textOn = textToSet2
			v.textOff = textToSet2
			v.setOnCheckedChangeListener { _, isChecked ->
				values[javaDayOfWeek] = isChecked
				if (onValuesChangeListener != null) {
					onValuesChangeListener!!.accept(values)
				}
			}
		}

		this.values = values // call setter
	}
	fun setOnValuesChangeListener(onValuesChangeListener: Consumer<BooleanArray>?) {
		this.onValuesChangeListener = onValuesChangeListener
	}
}