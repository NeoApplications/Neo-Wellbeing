package org.eu.droid_ng.wellbeing.ui

import android.animation.LayoutTransition
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.WellbeingService
import org.eu.droid_ng.wellbeing.prefs.PackageRecyclerViewAdapter
import org.eu.droid_ng.wellbeing.widget.MainSwitchBar
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
//        findViewById<View>(R.id.schedule)?.setOnClickListener {
//            startActivity(
//                Intent(
//                    this,
//                    ScheduleActivity::class.java
//                ).putExtra("type", "focus_mode").putExtra("name", getString(R.string.focus_mode))
//            )
//        }
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
        // Handle on click for main switch
        findViewById<MainSwitchBar>(R.id.mainSwitchBar)?.setOnClickListener {
            val state = service.getState()
            if (state.isFocusModeEnabled()) {
                service.disableFocusMode()
            } else {
                service.enableFocusMode()
            }
        }

        updateUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        service.removeStateCallback(stateCallback)
    }

    private fun updateUi() {
        val state = service.getState()
        updateMainSwitch(state.isFocusModeEnabled())
//        val takeBreak = findViewById<View>(R.id.takeBreak)
//        (findViewById<View>(R.id.title) as AppCompatTextView).setText(if (state.isOnFocusModeBreakGlobal()) R.string.focus_mode_break_end else R.string.focus_mode_break)
//        takeBreak.setOnClickListener { v: View? ->
//            if (state.isOnFocusModeBreakGlobal()) {
//                service.endFocusModeBreak()
//            } else {
//                service.takeFocusModeBreakWithDialog(this@FocusModeActivity, false, null)
//            }
//        }
//        takeBreak.visibility = if (state.isFocusModeEnabled()) View.VISIBLE else View.GONE
    }

    private fun updateMainSwitch(checked: Boolean) {
        findViewById<MainSwitchBar>(R.id.mainSwitchBar)?.isChecked = checked
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
