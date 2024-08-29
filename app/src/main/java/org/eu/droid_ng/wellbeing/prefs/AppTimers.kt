package org.eu.droid_ng.wellbeing.prefs

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.Utils
import org.eu.droid_ng.wellbeing.lib.Utils.clearUsageStatsCache
import org.eu.droid_ng.wellbeing.lib.Utils.getTimeUsed
import org.eu.droid_ng.wellbeing.lib.WellbeingService
import org.eu.droid_ng.wellbeing.lib.WellbeingService.Companion.get
import org.eu.droid_ng.wellbeing.prefs.AppTimers.AppTimersRecyclerViewAdapter.AppTimerViewHolder
import java.text.Collator
import java.time.Duration
import java.util.stream.Collectors

class AppTimers : AppCompatActivity() {
	private var ati: WellbeingService? = null
	private var h: Handler? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		h = Handler(mainLooper)
		ati = get()
		setContentView(R.layout.activity_app_timers)
		setSupportActionBar(findViewById(R.id.topbar))
		val actionBar = checkNotNull(supportActionBar)
		actionBar.setDisplayHomeAsUpEnabled(true)
		val r = findViewById<RecyclerView>(R.id.appTimerPkgs)
		Thread {
			val a = AppTimersRecyclerViewAdapter(
				this,
				ati!!.getInstalledApplications(PackageManager.GET_META_DATA)
			)
			h!!.post {
				findViewById<View>(R.id.appTimerLoading).visibility = View.GONE
				r.adapter = a
				r.visibility = View.VISIBLE
			}
		}.start()
	}

	override fun onSupportNavigateUp(): Boolean {
		finish()
		return true
	}

	inner class AppTimersRecyclerViewAdapter(context: Context, mData: List<ApplicationInfo>) :
		RecyclerView.Adapter<AppTimerViewHolder>() {
		private val inflater: LayoutInflater = LayoutInflater.from(context)
		private val mData: List<ApplicationInfo>
		private val pm: PackageManager = context.packageManager
		val prefs: SharedPreferences = context.getSharedPreferences("appTimers", 0)
		val enabledMap: MutableMap<String, Int> = HashMap()

		init {
			prefs.all.forEach { (k: String, v: Any?) ->
				if (v !is Int) {
					Log.e("OpenWellbeing", "Failed to parse $k")
					return@forEach
				}
				enabledMap[k] = v
			}
			val collator = Collator.getInstance()
			// Sort alphabetically by display name
			val nc = Comparator<ApplicationInfo> { a, b ->
				val durationA = getTimeUsed(
					ati!!.usm, a.packageName
				)
				val durationB = getTimeUsed(ati!!.usm, b.packageName)
				val x = durationA.compareTo(durationB)
				if (x != 0) return@Comparator -x
				val displayA: CharSequence = getAppNameForPkgName(a.packageName)
				val displayB: CharSequence = getAppNameForPkgName(b.packageName)
				collator.compare(displayA, displayB)
			}
			val mainIntent = Intent(Intent.ACTION_MAIN, null)
				.addCategory(Intent.CATEGORY_LAUNCHER)

			// We already force include user apps, so let's only iterate over system apps
			val hasLauncherIcon =
				pm.queryIntentActivities(mainIntent, PackageManager.MATCH_SYSTEM_ONLY)
					.stream().map { a: ResolveInfo -> a.activityInfo.packageName }
					.collect(Collectors.toList())
			this.mData = mData.stream().filter {
				// Filter out system apps without launcher icon and Default Launcher
				val isUser =
					(it.flags and (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM)) < 1
				!Utils.blackListedPackages.contains(it.packageName) && (isUser || hasLauncherIcon.contains(
					it.packageName
				))
			}.sorted { a, b ->
				// Enabled goes first
				val hasA = enabledMap.getOrDefault(a.packageName, 0) != 0
				val hasB = enabledMap.getOrDefault(b.packageName, 0) != 0
				if (hasA && hasB) return@sorted nc.compare(a, b)
				else if (hasA) return@sorted -1
				else if (hasB) return@sorted 1
				else return@sorted nc.compare(a, b)
			}.collect(Collectors.toList())
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppTimerViewHolder {
			val view = inflater.inflate(R.layout.appitem, parent, false)
			return AppTimerViewHolder(view)
		}

		override fun getItemCount(): Int {
			return mData.size
		}

		override fun onBindViewHolder(holder: AppTimerViewHolder, position: Int) {
			val i = mData[position]
			val mins = prefs.getInt(i.packageName, 0)
			holder.apply(i, mins)
		}

		inner class AppTimerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
			private val container: ViewGroup = itemView.findViewById(R.id.container)
			private val appIcon: AppCompatImageView = itemView.findViewById(R.id.appIcon)
			private val appName: AppCompatTextView = itemView.findViewById(R.id.appName2)
			private val appTimerInfo: AppCompatTextView = itemView.findViewById(R.id.pkgName)
			private val actionButton = AppCompatImageButton(itemView.context)

			init {
				actionButton.setImageDrawable(
					AppCompatResources.getDrawable(
						itemView.context, R.drawable.ic_focus_mode
					)
				)
				actionButton.background = null
				val checkBox = itemView.findViewById<MaterialCheckBox>(R.id.isChecked)
				val parent = checkBox.parent as ViewGroup
				val idx = parent.indexOfChild(checkBox)
				parent.removeView(checkBox)
				parent.addView(actionButton, idx)
			}

			fun apply(info: ApplicationInfo, mins: Int) {
				val restricted = Utils.restrictedPackages.contains(info.packageName)
				appIcon.setImageDrawable(getAppIconForPkgName(info.packageName))
				appName.text = getAppNameForPkgName(info.packageName)
				applyText(
					mins, Math.toIntExact(
						getTimeUsed(
							ati!!.usm, info.packageName
						).toMinutes()
					)
				)
				actionButton.isEnabled = !restricted
				container.setOnClickListener {
					if (restricted) return@setOnClickListener
					val realmins = enabledMap.getOrDefault(info.packageName, 0)
					val numberPicker = NumberPicker(this@AppTimers)
					numberPicker.minValue = 0
					numberPicker.maxValue = 9999 //i mean why not
					numberPicker.value = realmins
					AlertDialog.Builder(this@AppTimers)
						.setTitle(pm.getApplicationLabel(info))
						.setView(numberPicker)
						.setNegativeButton(R.string.cancel) { _, _ -> }
						.setPositiveButton(R.string.ok) { _, _ ->
							updateMins(info.packageName, realmins, numberPicker.value)
						}
						.show()
				}
			}

			private fun updateMins(pkgName: String, oldmins: Int, mins: Int) {
				enabledMap[pkgName] = mins
				prefs.edit().putInt(pkgName, mins).apply()
				applyText(
					mins, Math.toIntExact(
						getTimeUsed(
							ati!!.usm, pkgName
						).toMinutes()
					)
				)
				Thread {
					clearUsageStatsCache(ati!!.usm, pm, get().pmd, true)
					h!!.post {
						applyText(
							mins, Math.toIntExact(
								getTimeUsed(
									ati!!.usm, pkgName
								).toMinutes()
							)
						)
						ati!!.onUpdateAppTimerPreference(
							pkgName,
							Duration.ofMinutes(oldmins.toLong())
						)
					}
				}.start()
			}

			private fun applyText(mins: Int, mins2: Int) {
				appTimerInfo.text = itemView.context.getString(
					R.string.desc_container,
					if (mins == 0) itemView.context.getString(R.string.no_timer) else itemView.context.resources.getQuantityString(
						R.plurals.break_mins,
						mins,
						mins
					),
					itemView.context.resources.getQuantityString(R.plurals.break_mins, mins2, mins2)
				)
			}
		}
	}

	fun getAppNameForPkgName(tag: String): String {
		return appNames.computeIfAbsent(tag) { packageName ->
			val pm = packageManager
			try {
				val i = pm.getApplicationInfo(packageName, 0)
				return@computeIfAbsent pm.getApplicationLabel(i).toString()
			} catch (e: PackageManager.NameNotFoundException) {
				return@computeIfAbsent packageName
			}
		}
	}

	fun getAppIconForPkgName(tag: String): Drawable {
		return appIcons.computeIfAbsent(tag) { packageName ->
			val pm = packageManager
			try {
				return@computeIfAbsent pm.getApplicationIcon(packageName)
			} catch (e: PackageManager.NameNotFoundException) {
				return@computeIfAbsent AppCompatResources.getDrawable(
					this@AppTimers,
					android.R.drawable.sym_def_app_icon
				)!!
			}
		}
	}

	private val appIcons = hashMapOf<String, Drawable>()
	private val appNames = hashMapOf<String, String>()
}