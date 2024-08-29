package org.eu.droid_ng.wellbeing.prefs

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.BugUtils.Companion.BUG
import org.eu.droid_ng.wellbeing.lib.TimeChargerTriggerCondition
import org.eu.droid_ng.wellbeing.lib.Trigger
import org.eu.droid_ng.wellbeing.lib.WellbeingService.Companion.get
import java.util.function.Consumer

class ScheduleActivity : AppCompatActivity() {
	private var type: String? = null
	private lateinit var data: MutableList<Trigger>
	private var cardHost: LinearLayoutCompat? = null
	private var noCardNotification: AppCompatTextView? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val intent = intent
		type = null
		if (intent != null && intent.hasExtra("type")) {
			type = intent.getStringExtra("type")
		}
		if (type == null) {
			Log.e("ScheduleActivity", "intent or type is null")
			finish()
			return
		}
		setContentView(R.layout.activity_schedule)
		setSupportActionBar(findViewById(R.id.topbar))
		val actionBar = checkNotNull(supportActionBar)
		actionBar.setDisplayHomeAsUpEnabled(true)
		if (intent!!.hasExtra("name")) {
			actionBar.title = intent.getStringExtra("name")
		}
		findViewById<View>(R.id.floating_action_button).setOnClickListener {
			data.add(
				TimeChargerTriggerCondition(
					type!!,
					System.currentTimeMillis().toString(),
					true,
					7,
					0,
					18,
					0,
					booleanArrayOf(true, true, true, true, true, true, true),
					needCharger = false,
					endOnAlarm = false
				)
			)
			updateUi()
			updateServiceStatus()
		}
		cardHost = findViewById(R.id.cardHost)
		noCardNotification = AppCompatTextView(this)
		noCardNotification!!.setText(R.string.add_schedule_info)

		val tw = get()
		data = tw.getTriggersForId(type!!).toMutableList()
		updateUi()
	}

	private fun updateUi() {
		cardHost!!.removeAllViews()

		for (e in data) {
			if (e is TimeChargerTriggerCondition) {
				val scv = ScheduleCardView(this)
				scv.timeData = e
				scv.onValuesChangedCallback = Consumer { iid ->
					data.replaceAll {
						if (it.iid == iid) scv.timeData else it
					}
					updateUi()
					updateServiceStatus()
				}
				scv.onDeleteCardCallback = Consumer { iid ->
					data.removeIf { it.iid == iid }
					updateUi()
					updateServiceStatus()
				}
				cardHost!!.addView(scv)
				val m = LinearLayoutCompat.LayoutParams(scv.layoutParams)
				m.setMargins(
					0,
					TypedValue.applyDimension(
						TypedValue.COMPLEX_UNIT_DIP,
						10f,
						resources.displayMetrics
					).toInt(),
					0,
					0
				)
				scv.layoutParams = m
			} else {
				BUG("Cannot display " + e.javaClass.canonicalName)
			}
		}
		if (data.size < 1) {
			cardHost!!.addView(noCardNotification)
		}
	}

	private fun updateServiceStatus() {
		val tw = get()
		tw.setTriggersForId(type!!, data.toTypedArray<Trigger>())
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}
}