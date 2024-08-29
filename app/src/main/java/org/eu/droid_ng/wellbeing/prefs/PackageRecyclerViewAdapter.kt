package org.eu.droid_ng.wellbeing.prefs

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.Utils
import org.eu.droid_ng.wellbeing.prefs.PackageRecyclerViewAdapter.PackageNameViewHolder
import java.text.Collator
import java.util.function.Consumer
import java.util.stream.Collectors

internal class PackageRecyclerViewAdapter(
	private val mContext: Context,
	mData: List<ApplicationInfo>,
	private val settingsKey: String,
	private val callback: Consumer<String?>?
) : RecyclerView.Adapter<PackageNameViewHolder>() {
	private val inflater: LayoutInflater = LayoutInflater.from(mContext)
	private val mData: List<ApplicationInfo>
	private val enabledArr: MutableList<String?>
	private val pm: PackageManager = mContext.packageManager
	val prefs: SharedPreferences = mContext.getSharedPreferences("appLists", 0)

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageNameViewHolder {
		val view = inflater.inflate(R.layout.appitem, parent, false)
		return PackageNameViewHolder(view)
	}

	override fun getItemCount(): Int {
		return mData.size
	}

	override fun onBindViewHolder(holder: PackageNameViewHolder, position: Int) {
		holder.apply(mData[position].packageName)
	}

	inner class PackageNameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		private val container: View = itemView.findViewById(R.id.container)
		private val appIcon: ImageView = itemView.findViewById(R.id.appIcon)
		private val appName: TextView = itemView.findViewById(R.id.appName2)
		private val pkgName: TextView = itemView.findViewById(R.id.pkgName)
		private val checkBox: CheckBox = itemView.findViewById(R.id.isChecked)

		@SuppressLint("ApplySharedPref")
		fun apply(packageName: String?) {
			appIcon.setImageDrawable(getAppIconForPkgName(packageName))
			appName.text = getAppNameForPkgName(packageName)
			pkgName.text = packageName
			checkBox.isChecked = enabledArr.contains(packageName)
			container.setOnClickListener {
				var enabled = enabledArr.contains(packageName)
				enabled = !enabled
				checkBox.isChecked = enabled
				if (enabled) {
					enabledArr.add(packageName)
				} else {
					enabledArr.remove(packageName)
				}
				prefs.edit().putStringSet(settingsKey, HashSet(enabledArr)).commit()
				callback?.accept(packageName)
			}
		}
	}


	fun getAppNameForPkgName(tag: String?): String {
		return appNames.computeIfAbsent(tag) { packageName ->
			try {
				val i = pm.getApplicationInfo(packageName!!, 0)
				return@computeIfAbsent pm.getApplicationLabel(i).toString()
			} catch (e: PackageManager.NameNotFoundException) {
				return@computeIfAbsent packageName!!
			}
		}
	}

	fun getAppIconForPkgName(tag: String?): Drawable {
		return appIcons.computeIfAbsent(tag) { packageName: String? ->
			try {
				return@computeIfAbsent pm.getApplicationIcon(packageName!!)
			} catch (e: PackageManager.NameNotFoundException) {
				return@computeIfAbsent AppCompatResources.getDrawable(
					mContext,
					android.R.drawable.sym_def_app_icon
				)!!
			}
		}
	}

	private val appIcons: HashMap<String?, Drawable> = HashMap()
	private val appNames: HashMap<String?, String> = HashMap()

	init {
		val focusAppsS = prefs.getStringSet(this.settingsKey, HashSet())!!
		enabledArr = ArrayList(focusAppsS)
		val collator = Collator.getInstance()
		// Sort alphabetically by display name
		val nc = java.util.Comparator { a: ApplicationInfo, b: ApplicationInfo ->
			val displayA: CharSequence = getAppNameForPkgName(a.packageName)
			val displayB: CharSequence = getAppNameForPkgName(b.packageName)
			collator.compare(displayA, displayB)
		}
		val mainIntent = Intent(Intent.ACTION_MAIN, null)
			.addCategory(Intent.CATEGORY_LAUNCHER)

		// We already force include user apps, so let's only iterate over system apps
		val hasLauncherIcon = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_SYSTEM_ONLY)
			.stream().map { a: ResolveInfo -> a.activityInfo.packageName }
			.collect(Collectors.toList())
		this.mData = mData.stream().filter { i: ApplicationInfo ->
			// Filter out system apps without launcher icon and Settings, Dialer and Wellbeing
			val isUser =
				(i.flags and (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP or ApplicationInfo.FLAG_SYSTEM)) < 1
			!Utils.restrictedPackages.contains(i.packageName) && (isUser || hasLauncherIcon.contains(
				i.packageName
			))
		}.sorted { a: ApplicationInfo, b: ApplicationInfo ->
			// Enabled goes first
			val hasA = enabledArr.contains(a.packageName)
			val hasB = enabledArr.contains(b.packageName)
			if (hasA && hasB) return@sorted nc.compare(a, b)
			else if (hasA) return@sorted -1
			else if (hasB) return@sorted 1
			else return@sorted nc.compare(a, b)
		}.collect(Collectors.toList())
	}
}

