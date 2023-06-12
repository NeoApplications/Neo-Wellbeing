package org.eu.droid_ng.wellbeing.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import org.eu.droid_ng.wellbeing.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        // Set the toolbar as support action so we can access it from fragments.
        val topAppBar = findViewById<MaterialToolbar>(R.id.topbar)
        setSupportActionBar(topAppBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Launch settings preference fragment
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, MainPreferenceFragment())
                .commit()
        }
    }
}
