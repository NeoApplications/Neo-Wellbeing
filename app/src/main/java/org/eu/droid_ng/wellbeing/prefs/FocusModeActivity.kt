package org.eu.droid_ng.wellbeing.prefs

import android.animation.LayoutTransition
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.materialswitch.MaterialSwitch
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.WellbeingService
import org.eu.droid_ng.wellbeing.lib.WellbeingService.Companion.get
import java.util.function.Consumer

class FocusModeActivity : AppCompatActivity() {
	private val sc = Consumer<WellbeingService> { updateUi() }
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_focusmode)
		setSupportActionBar(findViewById(R.id.topbar))
		val actionBar = checkNotNull(supportActionBar)
		actionBar.setDisplayHomeAsUpEnabled(true)
		val layoutTransition =
			(findViewById<View>(R.id.focusModeRoot) as LinearLayoutCompat).layoutTransition
		layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

		findViewById<View>(R.id.schedule).setOnClickListener {
			startActivity(
				Intent(
					this, ScheduleActivity::class.java
				).putExtra("type", "focus_mode").putExtra("name", getString(R.string.focus_mode))
			)
		}

		val tw = get()
		tw.addStateCallback(sc)

		val r = findViewById<RecyclerView>(R.id.focusModePkgs)
		r.adapter = PackageRecyclerViewAdapter(
			this,
			tw.getInstalledApplications(PackageManager.GET_META_DATA),
			"focus_mode"
		) { packageName: String? ->
			tw.onFocusModePreferenceChanged(
				packageName!!
			)
		}

		updateUi()
	}

	override fun onDestroy() {
		super.onDestroy()
		val tw = get()
		tw.removeStateCallback(sc)
	}

	private fun updateUi() {
		val tw = get()
		val state = tw.getState()
		val toggle = findViewById<MaterialSwitch>(R.id.topsw)
		toggle.isChecked = state.isFocusModeEnabled()
		findViewById<View>(R.id.topsc).setOnClickListener {
			if (state.isFocusModeEnabled()) {
				tw.disableFocusMode()
			} else {
				tw.enableFocusMode()
			}
		}
		val takeBreak = findViewById<View>(R.id.takeBreak)
		(findViewById<View>(R.id.title) as AppCompatTextView).setText(if (state.isOnFocusModeBreakGlobal()) R.string.focus_mode_break_end else R.string.focus_mode_break)
		takeBreak.setOnClickListener {
			if (state.isOnFocusModeBreakGlobal()) {
				tw.endFocusModeBreak()
			} else {
				tw.takeFocusModeBreakWithDialog(this@FocusModeActivity, false, null)
			}
		}
		takeBreak.visibility = if (state.isFocusModeEnabled()) View.VISIBLE else View.GONE
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}
}