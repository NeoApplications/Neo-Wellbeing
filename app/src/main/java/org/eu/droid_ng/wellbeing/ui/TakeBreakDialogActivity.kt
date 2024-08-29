package org.eu.droid_ng.wellbeing.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.WellbeingService
import org.eu.droid_ng.wellbeing.lib.WellbeingService.Companion.get

class TakeBreakDialogActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.take_a_break_activity)
		setSupportActionBar(findViewById(R.id.topbar))
		val actionBar = checkNotNull(supportActionBar)
		actionBar.setDisplayHomeAsUpEnabled(true)

		val tw = get()
		val optionsS = WellbeingService.breakTimeOptions
			.map { i -> resources.getQuantityString(R.plurals.break_mins, i, i) }
			.toTypedArray()
		val a: ArrayAdapter<String> =
			object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, optionsS) {
				override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
					val v = super.getView(position, convertView, parent)
					v.setOnClickListener {
						tw.takeFocusModeBreak(WellbeingService.breakTimeOptions[position])
						this@TakeBreakDialogActivity.finish()
					}
					return v
				}
			}
		val lv = findViewById<ListView>(R.id.listView)
		lv.adapter = a
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}
}