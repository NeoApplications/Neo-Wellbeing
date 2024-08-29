package org.eu.droid_ng.wellbeing.ui

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.text.format.DateFormat
import android.util.ArrayMap
import android.util.Pair
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.WellbeingService.Companion.get
import org.eu.droid_ng.wellbeing.shared.ExactTime.ofUnit
import org.eu.droid_ng.wellbeing.shared.ExactTime.plus
import org.eu.droid_ng.wellbeing.shared.TimeDimension
import org.eu.droid_ng.wellbeing.ui.DashboardActivity.DashboardRecyclerViewAdapter.DashboardViewHolder
import java.text.Collator
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.function.BiFunction
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier

class DashboardActivity : AppCompatActivity() {
	private lateinit var ht: HandlerThread
	private lateinit var bgHandler: Handler
	private val appIcons = hashMapOf<String, Drawable>()
	private val appNames = hashMapOf<String, String>()
	private lateinit var whatStrings: Array<String?>
	private lateinit var whenStrings: Array<String?>
	private lateinit var thisStrings: Array<String>
	private var whatValue: Int = WhatStat.SCREEN_TIME.ordinal
	private var whenValue: Int = TimeDimension.DAY.ordinal
	private var mStart: LocalDateTime? = null
	private lateinit var chipWhen: Chip
	private lateinit var chipWhat: Chip
	private lateinit var chipStart: Chip
	private var didProcessTime = false

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		ht = HandlerThread("DashboardActivity")
		ht.start()
		bgHandler = Handler(ht.looper)
		setContentView(R.layout.activity_dashboard)
		setSupportActionBar(findViewById(R.id.topbar))
		val actionBar = checkNotNull(supportActionBar)
		actionBar.setDisplayHomeAsUpEnabled(true)
		whatStrings = resources.getStringArray(R.array.chip_what_entries)
		whenStrings = resources.getStringArray(R.array.chip_when_entries)
		thisStrings = resources.getStringArray(R.array.chip_this_entries)
		chipWhen = findViewById(R.id.chip_when)
		chipWhat = findViewById(R.id.chip_what)
		chipStart = findViewById(R.id.chip_start)
		chipWhen.setOnClickListener {
			showChipDialog(whenStrings, R.string.time_dimension_to_display, whenValue) { i: Int ->
				whenValue = i
				refresh(true)
			}
		}
		chipWhat.setOnClickListener {
			showChipDialog(whatStrings, R.string.stat_to_display, whatValue) { i: Int ->
				whatValue = i
				refresh(false)
			}
		}
		chipStart.setOnClickListener {
			val `when` = TimeDimension.entries[whenValue]
			val dp = MaterialDatePicker.Builder.datePicker()
				.setTitleText(R.string.select_date)
				.setSelection(
					mStart!!.withHour(12).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
				)
				.build()
			dp.addOnPositiveButtonClickListener { date: Long? ->
				if (`when` == TimeDimension.HOUR) {
					val tp = MaterialTimePicker.Builder()
						.setTitleText(R.string.select_hour)
						.setTimeFormat(if (DateFormat.is24HourFormat(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)
						.setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
						.setHour(mStart!!.hour)
						.setMinute(mStart!!.minute)
						.build()
					tp.addOnPositiveButtonClickListener {
						mStart = ofUnit(
							LocalDateTime.ofInstant(
								Instant.ofEpochMilli(
									date!!
								), ZoneId.systemDefault()
							)
								.withHour(tp.hour).withMinute(tp.minute), `when`
						)
						refresh(false)
					}
					tp.show(supportFragmentManager, "chipStartTime")
				} else {
					mStart = ofUnit(
						LocalDateTime.ofInstant(
							Instant.ofEpochMilli(
								date!!
							), ZoneId.systemDefault()
						), `when`
					)
					refresh(false)
				}
			}
			dp.show(supportFragmentManager, "chipStartDate")
		}
		refresh(true)
	}

	private fun refresh(resetDate: Boolean) {
		bgHandler.post {
			if (resetDate) mStart = ofUnit(LocalDateTime.now(), TimeDimension.entries[whenValue])
			updateLabels()
			showData {
				if (whatValue == WhatStat.SCREEN_TIME.ordinal && !didProcessTime) {
					get().onProcessStats(false) // takes a long time
					didProcessTime = true
				}
			}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		ht.quitSafely()
	}

	private fun updateLabels() {
		runOnUiThread {
			chipWhen.text = whenStrings[whenValue]
			chipWhat.text = whatStrings[whatValue]
			chipStart.text = fancyDate(mStart)
		}
	}

	private fun showChipDialog(
		values: Array<String?>?,
		title: Int,
		currentValue: Int,
		newValueConsumer: Consumer<Int>
	) {
		val atom = AtomicInteger(currentValue)
		AlertDialog.Builder(this)
			.setSingleChoiceItems(
				values,
				currentValue
			) { _, which -> atom.set(which) }
			.setTitle(title)
			.setNeutralButton(R.string.cancel) { _, _ -> }
			.setPositiveButton(R.string.ok) { _, _ ->
				newValueConsumer.accept(
					atom.get()
				)
			}
			.show()
	}

	private fun showData(preProcess: Runnable) {
		val what = WhatStat.entries[whatValue]
		val `when` = TimeDimension.entries[whenValue]
		showData(
			preProcess,
			what.isRemote,
			if (`when` != TimeDimension.HOUR) what.tName else null,
			what.prefix,
			`when`,
			mStart,
			plus(
				mStart!!, `when`, 1
			),
			getString(R.string.stat_view_name, whatStrings[whatValue], whenStrings[whenValue]),
			what.getSubtitleGenerator(
				this
			)
		)
	}

	private fun showData(
		preProcess: Runnable,
		remote: Boolean,
		id: String?,
		prefix: String?,
		dimension: TimeDimension,
		start: LocalDateTime?,
		end: LocalDateTime?,
		name: String?,
		subtitleGenerator: BiFunction<String, Long, String?>?
	) {
		bgHandler.post {
			showData(preProcess,
				if (prefix == null) null else Supplier<Map<String, Long>> {
					if (remote) get().getRemoteEventStatsByPrefix(prefix, dimension, start!!, end!!)
					else get().getEventStatsByPrefix(prefix, dimension, start!!, end!!)
				},
				if (id == null) null else Supplier<Map<Int, Long>> {
					val count: MutableMap<Int, Long> = ArrayMap()
					val myDimension = TimeDimension.entries[dimension.ordinal + 1]
					var newStart = start
					var newEnd = plus(newStart!!, myDimension, 1)
					var count2 =
						if (myDimension == TimeDimension.HOUR) 0 else 1 // hours start at 0 (midnight); days and months at 1
					while ((newStart!!.isAfter(start) || newStart.isEqual(start)) && newStart.isBefore(
							end
						) && newEnd.isAfter(start) && (newEnd.isBefore(end) || newEnd.isEqual(end))
					) {
						count[count2++] = if (remote) get().getRemoteEventStatsByType(
							id,
							myDimension,
							newStart,
							newEnd
						)
						else get().getEventStatsByType(id, myDimension, newStart, newEnd)
						newStart = plus(newStart, myDimension, 1)
						newEnd = plus(newEnd, myDimension, 1)
					}
					count
				},
				name,
				if (prefix == null) null else Function { tag: String -> tag.substring(prefix.length) },
				subtitleGenerator
			)
		}
	}

	private fun showData(
		preProcess: Runnable,
		rawDataGenerator: Supplier<Map<String, Long>>?,
		rawData2Generator: Supplier<Map<Int, Long>>?,
		desc: String?,
		packageNameGenerator: Function<String, String>?,
		subtitleGenerator: BiFunction<String, Long, String?>?
	) {
		runOnUiThread {
			findViewById<View>(R.id.dashboardLoading).visibility = View.VISIBLE
			findViewById<View>(R.id.dashboardContainer).visibility = View.GONE
		}
		preProcess.run()
		val bar = findViewById<BarChart>(R.id.chart)
		val pie = findViewById<PieChart>(R.id.chart2)
		val rawData: Map<String, Long>? = rawDataGenerator?.get()
		val rawData2: Map<Int, Long>? = rawData2Generator?.get()
		val pieEntries: MutableList<PieEntry> = ArrayList()
		rawData?.forEach { (tag, count) ->
			pieEntries.add(
				PieEntry(
					count.toFloat(), getAppNameForPkgName(
						packageNameGenerator!!.apply(tag)
					)
				)
			)
		}
		pieEntries.sortWith { a, b ->
			b.value.compareTo(a.value)
		}
		if (pieEntries.size > 4) {
			val count = AtomicLong()
			val others = pieEntries.subList(4, pieEntries.size)
			others.forEach(Consumer { p: PieEntry -> count.addAndGet(p.value.toLong()) })
			others.clear()
			pieEntries.add(PieEntry(count.get().toFloat(), "Other"))
		}
		runOnUiThread {
			val set = PieDataSet(pieEntries, "")
			set.setColors(*ColorTemplate.JOYFUL_COLORS)
			val data = PieData(set)
			pie.data = data
			pie.description.text = desc

			pie.data.setValueTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface))
			pie.setEntryLabelColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface))
			//pie.getDescription().setTextSize(getTextSize(com.google.android.material.R.attr.tabTextAppearance));
			pie.description.textColor =
				getAttrColor(com.google.android.material.R.attr.colorOnSurface)
			pie.setHoleColor(getAttrColor(com.google.android.material.R.attr.colorSurface))
			pie.legend.textColor = getAttrColor(com.google.android.material.R.attr.colorOnSurface)
			pie.legend.isWordWrapEnabled = true
			pie.invalidate() // refresh
		}
		val barEntries: MutableList<BarEntry> = ArrayList()
		val total2 = AtomicLong()
		rawData2?.forEach { (tag: Int, count: Long) ->
			barEntries.add(BarEntry(tag.toFloat(), count.toFloat()))
			total2.addAndGet(count)
		}
		runOnUiThread {
			val set = BarDataSet(barEntries, "")
			set.setColors(*ColorTemplate.MATERIAL_COLORS)
			val data = BarData(set)
			bar.data = data
			bar.description.text = getString(R.string.total, total2.get())

			bar.xAxis.textColor = getAttrColor(com.google.android.material.R.attr.colorOnSurface)
			bar.axisLeft.textColor = getAttrColor(com.google.android.material.R.attr.colorOnSurface)
			bar.axisRight.textColor =
				getAttrColor(com.google.android.material.R.attr.colorOnSurface)
			bar.data.setValueTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface))
			//bar.getDescription().setTextSize(getTextSize(com.google.android.material.R.attr.tabTextAppearance));
			bar.description.textColor =
				getAttrColor(com.google.android.material.R.attr.colorOnSurface)
			bar.legend.textColor = getAttrColor(com.google.android.material.R.attr.colorOnSurface)
			bar.legend.isWordWrapEnabled = true
			bar.invalidate() // refresh
		}

		val r = findViewById<RecyclerView>(R.id.dashboardPkgs)
		val adapter: RecyclerView.Adapter<*>? = if (rawData != null) {
			DashboardRecyclerViewAdapter(this, rawData, packageNameGenerator, subtitleGenerator)
		} else {
			null
		}
		runOnUiThread {
			if (rawData != null) {
				pie.visibility = View.VISIBLE
			} else {
				pie.visibility = View.GONE
			}
			if (rawData2 != null) {
				bar.visibility = View.VISIBLE
			} else {
				bar.visibility = View.GONE
			}
			if (pieEntries.isEmpty() && barEntries.isEmpty()) {
				findViewById<View>(R.id.noData).visibility = View.VISIBLE
			} else {
				findViewById<View>(R.id.noData).visibility = View.GONE
			}
			r.adapter = adapter
			findViewById<View>(R.id.dashboardLoading).visibility = View.GONE
			findViewById<View>(R.id.dashboardContainer).visibility = View.VISIBLE
		}
	}

	inner class DashboardRecyclerViewAdapter(
		context: Context?,
		data: Map<String, Long>,
		packageNameGenerator: Function<String, String>?,
		private val subtitleGenerator: BiFunction<String, Long, String?>?
	) : RecyclerView.Adapter<DashboardViewHolder>() {
		private val inflater: LayoutInflater = LayoutInflater.from(context)
		private val mData: MutableList<Pair<String, Long>> = ArrayList()

		init {
			val collator = Collator.getInstance()

			data.forEach { (i, j) ->
				val include = j > 0
				if (include) mData.add(
					Pair(
						packageNameGenerator!!.apply(i), j
					)
				)
			}
			mData.sortWith { a, b ->
				val countA = a.second
				val countB = b.second
				val x = if (countA == null || countB == null) 0 else countA.compareTo(countB)
				if (x != 0) return@sortWith -x
				val displayA: CharSequence = getAppNameForPkgName(a.first)
				val displayB: CharSequence = getAppNameForPkgName(b.first)
				collator.compare(displayA, displayB)
			}
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashboardViewHolder {
			val view = inflater.inflate(R.layout.appitem, parent, false)
			return DashboardViewHolder(view)
		}

		override fun getItemCount(): Int {
			return mData.size
		}

		override fun onBindViewHolder(holder: DashboardViewHolder, position: Int) {
			val i = mData[position]
			holder.apply(i)
		}

		inner class DashboardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
			private val appIcon: AppCompatImageView = itemView.findViewById(R.id.appIcon)
			private val appName: AppCompatTextView = itemView.findViewById(R.id.appName2)
			private val subtitle: AppCompatTextView = itemView.findViewById(R.id.pkgName)

			init {
				val actionButton = AppCompatImageButton(itemView.context)
				actionButton.setImageDrawable(
					AppCompatResources.getDrawable(
						itemView.context, R.drawable.ic_focus_mode
					)
				)
				actionButton.background = null
				val checkBox = itemView.findViewById<MaterialCheckBox>(R.id.isChecked)
				val parent = checkBox.parent as ViewGroup
				//val idx = parent.indexOfChild(checkBox)
				parent.removeView(checkBox)
				//parent.addView(actionButton, idx)
			}

			fun apply(info: Pair<String, Long>) {
				appIcon.setImageDrawable(getAppIconForPkgName(info.first))
				appName.text = getAppNameForPkgName(info.first)
				subtitle.text = subtitleGenerator!!.apply(info.first, info.second)
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
					this@DashboardActivity,
					android.R.drawable.sym_def_app_icon
				)!!
			}
		}
	}

	private fun getAttrColor(attr: Int): Int {
		val typedValue = TypedValue()
		val theme = theme
		theme.resolveAttribute(attr, typedValue, true)
		@ColorInt val color = typedValue.data
		return color
	}

	private fun getTextSize(size: Int): Int {
		val typedValue = TypedValue()
		theme.resolveAttribute(size, typedValue, true)
		val textSizeAttr = intArrayOf(android.R.attr.textSize)
		val a = obtainStyledAttributes(typedValue.data, textSizeAttr)
		val textSize = a.getDimensionPixelSize(0, -1)
		a.recycle()
		return textSize
	}

	enum class WhatStat(
		val tName: String,
		val prefix: String?,
		val isRemote: Boolean,
		private val subtitleGeneratorGenerator: (Context) -> BiFunction<String, Long, String?>?
	) {
		SCREEN_TIME(
			"usage",
			"usage_",
			false,
			{ ctx ->
				BiFunction { _, count ->
					ctx.resources.getQuantityString(
						R.plurals.break_mins,
						count.toInt(),
						count.toInt()
					)
				}
			}),
		NOTIFICATIONS(
			"notif",
			"notif_",
			true,
			{ ctx ->
				BiFunction { _, count ->
					ctx.resources.getQuantityString(
						R.plurals.notifications_count,
						count.toInt(), count
					)
				}
			}),
		UNLOCK("unlock", null, true, { null });

		fun getSubtitleGenerator(context: Context): BiFunction<String, Long, String?>? {
			return subtitleGeneratorGenerator(context)
		}
	}

	// "Today"/"This week"/etc or locale-sensible date
	private fun fancyDate(start: LocalDateTime?): String {
		if (plus(
				LocalDateTime.now(),
				TimeDimension.entries[whenValue],
				-1
			).isBefore(start) && !start!!.isAfter(
				LocalDateTime.now()
			)
		) {
			return thisStrings[whenValue]
		}
		if (whenValue == TimeDimension.HOUR.ordinal) {
			return SimpleDateFormat.getDateTimeInstance().format(
				Date(
					start!!.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L
				)
			)
		}
		return SimpleDateFormat.getDateInstance()
			.format(Date(start!!.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L))
	}
}