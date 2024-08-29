package org.eu.droid_ng.wellbeing.prefs

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.materialswitch.MaterialSwitch
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.WellbeingService
import org.eu.droid_ng.wellbeing.lib.WellbeingService.Companion.get
import java.util.function.Consumer

class BedtimeMode : AppCompatActivity() {
	private val sc = Consumer { tw: WellbeingService ->
		val bt = findViewById<MaterialSwitch>(R.id.topsw)
		bt.isChecked = tw.getState(false).isBedtimeModeEnabled()
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_bedtime_mode)
		setSupportActionBar(findViewById(R.id.topbar))
		val actionBar = checkNotNull(supportActionBar)
		actionBar.setDisplayHomeAsUpEnabled(true)
		val tw = get()
		val prefs = getSharedPreferences("bedtime_mode", 0)

		val bt = findViewById<MaterialSwitch>(R.id.topsw)
		findViewById<View>(R.id.topsc).setOnClickListener { v: View? ->
			val b = !tw.getState(false).isBedtimeModeEnabled()
			tw.setBedtimeMode(b)
			bt.isChecked = b
		}
		bt.isChecked = tw.getState(false).isBedtimeModeEnabled()
		val checkBox2 = findViewById<MaterialCheckBox>(R.id.checkBox2)
		checkBox2.isChecked = prefs.getBoolean("greyscale", false)
		findViewById<View>(R.id.greyscaleCheckbox).setOnClickListener { v: View? ->
			val b = !prefs.getBoolean("greyscale", false)
			checkBox2.isChecked = b
			val g = tw.getState(false).isBedtimeModeEnabled()
			prefs.edit().putBoolean("greyscale", b).apply()
			if (g) {
				tw.cdm.setSaturationLevel(if (b) 0 else 100)
			}
		}
		val checkBox3 = findViewById<MaterialCheckBox>(R.id.checkBox3)
		checkBox3.isChecked = prefs.getBoolean("airplane_mode", false)
		findViewById<View>(R.id.airplaneModeCheckbox).setOnClickListener { v: View? ->
			val b = !prefs.getBoolean("airplane_mode", false)
			checkBox3.isChecked = b
			val g = tw.getState(false).isBedtimeModeEnabled()
			prefs.edit().putBoolean("airplane_mode", b).apply()
			if (g) {
				tw.setWellbeingAirplaneMode(b)
			}
		}
		findViewById<View>(R.id.schedule).setOnClickListener { v: View? ->
			startActivity(
				Intent(
					this, ScheduleActivity::class.java
				).putExtra("type", "bedtime_mode")
					.putExtra("name", getString(R.string.bedtime_mode))
			)
		}

		//TODO: do not disturb
		//TODO: disable AOD(A11)
		//TODO: dim the wallpaper(A13)
		//TODO: dark theme(A13)
		tw.addStateCallback(sc)
	}

	override fun onDestroy() {
		super.onDestroy()
		val tw = get()
		tw.removeStateCallback(sc)
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}
}