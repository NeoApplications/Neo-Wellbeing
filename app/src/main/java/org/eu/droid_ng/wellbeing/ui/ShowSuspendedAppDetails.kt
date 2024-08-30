package org.eu.droid_ng.wellbeing.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.card.MaterialCardView
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.shared.BugUtils.Companion.BUG
import org.eu.droid_ng.wellbeing.lib.WellbeingService
import org.eu.droid_ng.wellbeing.lib.WellbeingService.Companion.get
import org.eu.droid_ng.wellbeing.shim.PackageManagerDelegate

class ShowSuspendedAppDetails : AppCompatActivity() {
	private var tw: WellbeingService? = null
	private var pmd: PackageManagerDelegate? = null

	@SuppressLint("SetTextI18n")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		val packageName = intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)
		if (packageName == null) {
			Toast.makeText(
				this@ShowSuspendedAppDetails,
				"Assertion failure (0xAB): packageName is null. Please report this to the developers!",
				Toast.LENGTH_LONG
			).show()
			BUG("packageName == null (0xAB)")
			finish()
			return
		}
		tw = get()
		val pm = packageManager
		pmd = PackageManagerDelegate(pm)

		setContentView(R.layout.activity_show_suspended_app_details)
		setSupportActionBar(findViewById(R.id.topbar))
		val actionBar = checkNotNull(supportActionBar)
		actionBar.setDisplayHomeAsUpEnabled(true)

		val iconView = findViewById<AppCompatImageView>(R.id.appIcon)
		val nameView = findViewById<AppCompatTextView>(R.id.appName)
		var appInfo: ApplicationInfo? = null
		var icon: Drawable? = null
		var name: CharSequence? = null
		try {
			appInfo = tw!!.getApplicationInfo(packageName, false)
			icon = pm.getApplicationIcon(appInfo)
			name = pm.getApplicationLabel(appInfo)
		} catch (ignored: PackageManager.NameNotFoundException) {
		}
		if (appInfo != null && icon != null && name != null) {
			iconView.setImageDrawable(icon)
			nameView.text = name
		}
		val reason = tw!!.getAppState(packageName)
		var container: MaterialCardView
		var hasReason = 0
		if (reason.isAppTimerExpired() && !reason.isAppTimerBreak()) {
			hasReason++
			container = findViewById(R.id.apptimer)
			findViewById<View>(R.id.takeabreakbtn2).setOnClickListener { v: View? ->
				tw!!.takeAppTimerBreakWithDialog(
					this@ShowSuspendedAppDetails, true, arrayOf(packageName)
				)
			}
			container.visibility = View.VISIBLE
		}
		if (reason.isFocusModeEnabled() && !(reason.isOnFocusModeBreakGlobal() || reason.isOnFocusModeBreakPartial())) {
			hasReason++
			container = findViewById(R.id.focusMode)
			findViewById<View>(R.id.takeabreakbtn).setOnClickListener { v: View? ->
				tw!!.takeFocusModeBreakWithDialog(
					this@ShowSuspendedAppDetails,
					true,
					if (tw!!.focusModeAllApps) null else arrayOf(packageName)
				)
			}
			findViewById<View>(R.id.disablefocusmode).setOnClickListener { v: View? ->
				tw!!.disableFocusMode()
				this@ShowSuspendedAppDetails.finish()
			}
			container.visibility = View.VISIBLE
		}
		if (reason.isSuspendedManually()) {
			hasReason++
			container = findViewById(R.id.manually)
			findViewById<View>(R.id.unsuspendbtn2).setOnClickListener { v: View? ->
				tw!!.manualUnsuspend(arrayOf(packageName))
				this@ShowSuspendedAppDetails.finish()
			}
			findViewById<View>(R.id.unsuspendallbtn).setOnClickListener { v: View? ->
				tw!!.manualUnsuspend(null)
				this@ShowSuspendedAppDetails.finish()
			}
			container.visibility = View.VISIBLE
		}
		if (hasReason < 1 || reason.hasUpdateFailed()) {
			container = findViewById(R.id.unknown)
			findViewById<View>(R.id.unsuspendbtn).setOnClickListener { v: View? ->
				BUG("Used unknown unsuspend!!")
				pmd!!.setPackagesSuspended(arrayOf(packageName), false, null, null, null)
				this@ShowSuspendedAppDetails.finish()
			}
			container.visibility = View.VISIBLE
		}
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}
}