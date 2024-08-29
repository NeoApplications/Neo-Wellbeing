package org.eu.droid_ng.wellbeing.prefs

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.WellbeingService.Companion.get

class ManualSuspendActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_manual_suspend)
		setSupportActionBar(findViewById(R.id.topbar))
		val actionBar = checkNotNull(supportActionBar)
		actionBar.setDisplayHomeAsUpEnabled(true)
		val suspendbtn = findViewById<MaterialButton>(R.id.suspendbtn)
		val unsuspendbtn = findViewById<MaterialButton>(R.id.desuspendbtn)
		val pkgList = findViewById<RecyclerView>(R.id.pkgList)
		val a: PackageRecyclerViewAdapter
		pkgList.adapter = PackageRecyclerViewAdapter(
			this,
			packageManager.getInstalledApplications(PackageManager.GET_META_DATA),
			"manual_suspend", null
		).also { a = it }
		val tw = get()
		suspendbtn.setOnClickListener { v: View? -> tw.manualSuspend(null) }
		unsuspendbtn.setOnClickListener { v: View? ->
			tw.manualUnsuspend(
				a.prefs.getStringSet("manual_suspend", HashSet())!!.toTypedArray<String>()
			)
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}
}