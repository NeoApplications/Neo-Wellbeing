package org.eu.droid_ng.wellbeing.ui

import android.animation.LayoutTransition
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.WellbeingService
import org.eu.droid_ng.wellbeing.prefs.PackageRecyclerViewAdapter
import org.eu.droid_ng.wellbeing.prefs.ScheduleActivity
import java.util.function.Consumer

class FocusModeActivity : AppCompatActivity() {

    private val service: WellbeingService by lazy {
        WellbeingService.get()
    }

    private val stateCallback = Consumer<WellbeingService> { _: WellbeingService ->
        updateUi()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_focus_mode)
        // Set support action bar
        val topAppBar = findViewById<MaterialToolbar>(R.id.topbar)
        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Enable layout transition
        val layoutTransition =
            (findViewById<View>(R.id.focusModeRoot) as LinearLayoutCompat).layoutTransition
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        // Open schedule screen
        findViewById<View>(R.id.schedule).setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ScheduleActivity::class.java
                ).putExtra("type", "focus_mode").putExtra("name", getString(R.string.focus_mode))
            )
        }
        // Add state call back to the service
        service.addStateCallback(stateCallback)
        val r = findViewById<RecyclerView>(R.id.focusModePkgs)
        r.adapter = PackageRecyclerViewAdapter(
            this,
            service.getInstalledApplications(PackageManager.GET_META_DATA),
            "focus_mode"
        ) { packageName: String? ->
            service.onFocusModePreferenceChanged(
                packageName!!
            )
        }
        updateUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        service.removeStateCallback(stateCallback)
    }

    private fun updateUi() {
        val state = service.getState()
        val toggle = findViewById<MaterialSwitch>(R.id.topsw)
        toggle.isChecked = state.isFocusModeEnabled()
        findViewById<View>(R.id.topsc).setOnClickListener { v: View? ->
            if (state.isFocusModeEnabled()) {
                service.disableFocusMode()
            } else {
                service.enableFocusMode()
            }
        }
        val takeBreak = findViewById<View>(R.id.takeBreak)
        (findViewById<View>(R.id.title) as AppCompatTextView).setText(if (state.isOnFocusModeBreakGlobal()) R.string.focus_mode_break_end else R.string.focus_mode_break)
        takeBreak.setOnClickListener { v: View? ->
            if (state.isOnFocusModeBreakGlobal()) {
                service.endFocusModeBreak()
            } else {
                service.takeFocusModeBreakWithDialog(this@FocusModeActivity, false, null)
            }
        }
        takeBreak.visibility = if (state.isFocusModeEnabled()) View.VISIBLE else View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
