package org.eu.droid_ng.wellbeing.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.format.DateFormat;
import android.util.ArrayMap;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.chip.Chip;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;
import org.eu.droid_ng.wellbeing.shared.ExactTime;
import org.eu.droid_ng.wellbeing.shared.TimeDimension;

import java.text.Collator;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class DashboardActivity extends AppCompatActivity {

	public HandlerThread ht;
	public Handler bgHandler;
	public final HashMap<String, Drawable> appIcons = new HashMap<>();
	public final HashMap<String, String> appNames = new HashMap<>();
	public String[] whatStrings;
	public String[] whenStrings;
	public String[] thisStrings;
	public int whatValue = WhatStat.NOTIFICATIONS.ordinal();
	public int whenValue = TimeDimension.DAY.ordinal();
	public LocalDateTime mStart;
	private Chip chipWhen, chipWhat, chipStart;
	private boolean didProcessTime = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ht = new HandlerThread("DashboardActivity");
		ht.start();
		bgHandler = new Handler(ht.getLooper());
		setContentView(R.layout.activity_dashboard);
		setSupportActionBar(findViewById(R.id.topbar));
		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);
		whatStrings = getResources().getStringArray(R.array.chip_what_entries);
		whenStrings = getResources().getStringArray(R.array.chip_when_entries);
		thisStrings = getResources().getStringArray(R.array.chip_this_entries);
		chipWhen = findViewById(R.id.chip_when);
		chipWhat = findViewById(R.id.chip_what);
		chipStart = findViewById(R.id.chip_start);
		chipWhen.setOnClickListener(v -> showChipDialog(whenStrings, R.string.time_dimension_to_display, whenValue, i -> {
			whenValue = i;
			refresh(true);
		}));
		chipWhat.setOnClickListener(v -> showChipDialog(whatStrings, R.string.stat_to_display, whatValue, i -> {
			whatValue = i;
			refresh(false);
		}));
		chipStart.setOnClickListener(v -> {
			TimeDimension when = TimeDimension.values()[whenValue];
			MaterialDatePicker<Long> dp = MaterialDatePicker.Builder.datePicker()
					.setTitleText(R.string.select_date)
					.setSelection(mStart.withHour(12).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
					.build();
			dp.addOnPositiveButtonClickListener(date -> {
				if (when == TimeDimension.HOUR) {
					MaterialTimePicker tp = new MaterialTimePicker.Builder()
							.setTitleText(R.string.select_hour)
							.setTimeFormat(DateFormat.is24HourFormat(this) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
							.setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
							.setHour(mStart.getHour())
							.setMinute(mStart.getMinute())
							.build();
					tp.addOnPositiveButtonClickListener(vv -> {
						mStart = ExactTime.INSTANCE.ofUnit(LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault())
								.withHour(tp.getHour()).withMinute(tp.getMinute()), when);
						refresh(false);
					});
					tp.show(getSupportFragmentManager(), "chipStartTime");
				} else {
					mStart = ExactTime.INSTANCE.ofUnit(LocalDateTime.ofInstant(Instant.ofEpochMilli(date), ZoneId.systemDefault()), when);
					refresh(false);
				}
			});
			dp.show(getSupportFragmentManager(), "chipStartDate");
		});
		refresh(true);
	}

	public void refresh(boolean resetDate) {
		bgHandler.post(() -> {
			if (resetDate)
				mStart = ExactTime.INSTANCE.ofUnit(LocalDateTime.now(), TimeDimension.values()[whenValue]);
			updateLabels();
			showData(() -> {
				if (whatValue == WhatStat.SCREEN_TIME.ordinal() && !didProcessTime) {
					WellbeingService.get().onProcessStats(false); // takes a long time
					didProcessTime = true;
				}
			});
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ht.quitSafely();
	}

	public void updateLabels() {
		runOnUiThread(() -> {
			chipWhen.setText(whenStrings[whenValue]);
			chipWhat.setText(whatStrings[whatValue]);
			chipStart.setText(fancyDate(mStart));
		});
	}

	public void showChipDialog(String[] values, int title, int currentValue, Consumer<Integer> newValueConsumer) {
		AtomicInteger atom = new AtomicInteger(currentValue);
		new AlertDialog.Builder(this)
				.setSingleChoiceItems(values, currentValue, (dialog, which) -> atom.set(which))
				.setTitle(title)
				.setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
				.setPositiveButton(R.string.ok, (dialog, which) -> newValueConsumer.accept(atom.get()))
				.show();
	}

	public void showData(Runnable preProcess) {
		WhatStat what = WhatStat.values()[whatValue];
		TimeDimension when = TimeDimension.values()[whenValue];
		showData(preProcess, what.isRemote(), when != TimeDimension.HOUR ? what.getName() : null, what.getPrefix(), when, mStart, ExactTime.INSTANCE.plus(mStart, when, 1),
				getString(R.string.stat_view_name, whatStrings[whatValue], whenStrings[whenValue]), what.getSubtitleGenerator(this));
	}

	public void showData(Runnable preProcess, boolean remote, String id, String prefix, TimeDimension dimension, LocalDateTime start,
	                     LocalDateTime end, String name, BiFunction<String, Long, String> subtitleGenerator) {
		bgHandler.post(() -> showData(preProcess,
				prefix == null ? null : () -> remote ? WellbeingService.get().getRemoteEventStatsByPrefix(prefix, dimension, start, end)
						: WellbeingService.get().getEventStatsByPrefix(prefix, dimension, start, end),
				id == null ? null : () -> {
					Map<Integer, Long> count = new ArrayMap<>();
					TimeDimension myDimension = TimeDimension.values()[dimension.ordinal() + 1];
					LocalDateTime newStart = start;
					LocalDateTime newEnd = ExactTime.INSTANCE.plus(newStart, myDimension, 1);
					int count2 = myDimension == TimeDimension.HOUR ? 0 : 1; // hours start at 0 (midnight); days and months at 1
					while ((newStart.isAfter(start) || newStart.isEqual(start)) && newStart.isBefore(end) && newEnd.isAfter(start) && (newEnd.isBefore(end) || newEnd.isEqual(end))) {
						count.put(count2++, remote ? WellbeingService.get().getRemoteEventStatsByType(id, myDimension, newStart, newEnd)
								: WellbeingService.get().getEventStatsByType(id, myDimension, newStart, newEnd));
						newStart = ExactTime.INSTANCE.plus(newStart, myDimension, 1);
						newEnd = ExactTime.INSTANCE.plus(newEnd, myDimension, 1);
					}
					return count;
				},
				name, prefix == null ? null : tag -> tag.substring(prefix.length()), subtitleGenerator));
	}

	public void showData(Runnable preProcess, Supplier<Map<String, Long>> rawDataGenerator, Supplier<Map<Integer, Long>> rawData2Generator,
	                     String desc, Function<String, String> packageNameGenerator, BiFunction<String, Long, String> subtitleGenerator) {
		runOnUiThread(() -> {
			findViewById(R.id.dashboardLoading).setVisibility(View.VISIBLE);
			findViewById(R.id.dashboardContainer).setVisibility(View.GONE);
		});
		preProcess.run();
		final BarChart bar = findViewById(R.id.chart);
		final PieChart pie = findViewById(R.id.chart2);
		final Map<String, Long> rawData = rawDataGenerator != null ? rawDataGenerator.get() : null;
		final Map<Integer, Long> rawData2 = rawData2Generator != null ? rawData2Generator.get() : null;
		List<PieEntry> pieEntries = new ArrayList<>();
		if (rawData != null) {
			rawData.forEach((tag, count) -> pieEntries.add(new PieEntry(count, getAppNameForPkgName(packageNameGenerator.apply(tag)))));
		}
		pieEntries.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
		if (pieEntries.size() > 4) {
			AtomicLong count = new AtomicLong();
			List<PieEntry> others = pieEntries.subList(4, pieEntries.size());
			others.forEach(p -> count.addAndGet((long) p.getValue()));
			others.clear();
			pieEntries.add(new PieEntry(count.get(), "Other"));
		}
		runOnUiThread(() -> {
			PieDataSet set = new PieDataSet(pieEntries, "");
			set.setColors(ColorTemplate.JOYFUL_COLORS);
			PieData data = new PieData(set);
			pie.setData(data);
			pie.getDescription().setText(desc);

			pie.getData().setValueTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
			pie.setEntryLabelColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
			//pie.getDescription().setTextSize(getTextSize(com.google.android.material.R.attr.tabTextAppearance));
			pie.getDescription().setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
			pie.setHoleColor(getAttrColor(com.google.android.material.R.attr.colorSurface));
			pie.getLegend().setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
			pie.getLegend().setWordWrapEnabled(true);
			pie.invalidate(); // refresh
		});
		List<BarEntry> barEntries = new ArrayList<>();
		AtomicLong total2 = new AtomicLong();
		if (rawData2 != null) {
			rawData2.forEach((tag, count) -> {
				barEntries.add(new BarEntry(tag, count));
				total2.addAndGet(count);
			});
		}
		runOnUiThread(() -> {
			BarDataSet set = new BarDataSet(barEntries, "");
			set.setColors(ColorTemplate.MATERIAL_COLORS);
			BarData data = new BarData(set);
			bar.setData(data);
			bar.getDescription().setText(getString(R.string.total, total2.get()));

			bar.getXAxis().setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
			bar.getAxisLeft().setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
			bar.getAxisRight().setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
			bar.getData().setValueTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
			//bar.getDescription().setTextSize(getTextSize(com.google.android.material.R.attr.tabTextAppearance));
			bar.getDescription().setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
			bar.getLegend().setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
			bar.getLegend().setWordWrapEnabled(true);
			bar.invalidate(); // refresh
		});

		final RecyclerView r = findViewById(R.id.dashboardPkgs);
		final RecyclerView.Adapter<?> adapter;
		if (rawData != null) {
			adapter = new DashboardRecyclerViewAdapter(this, rawData, packageNameGenerator, subtitleGenerator);
		} else {
			adapter = null;
		}
		runOnUiThread(() -> {
			if (rawData != null) {
				pie.setVisibility(View.VISIBLE);
			} else {
				pie.setVisibility(View.GONE);
			}
			if (rawData2 != null) {
				bar.setVisibility(View.VISIBLE);
			} else {
				bar.setVisibility(View.GONE);
			}
			if (pieEntries.isEmpty() && barEntries.isEmpty()) {
				findViewById(R.id.noData).setVisibility(View.VISIBLE);
			} else {
				findViewById(R.id.noData).setVisibility(View.GONE);
			}
			r.setAdapter(adapter);
			findViewById(R.id.dashboardLoading).setVisibility(View.GONE);
			findViewById(R.id.dashboardContainer).setVisibility(View.VISIBLE);
		});
	}

	public class DashboardRecyclerViewAdapter extends RecyclerView.Adapter<DashboardRecyclerViewAdapter.DashboardViewHolder> {
		private final BiFunction<String, Long, String> subtitleGenerator;
		private final LayoutInflater inflater;
		private final List<Pair<String, Long>> mData = new ArrayList<>();

		public DashboardRecyclerViewAdapter(Context context, Map<String, Long> data, Function<String, String> packageNameGenerator, BiFunction<String, Long, String> subtitleGenerator) {
			this.subtitleGenerator = subtitleGenerator;
			this.inflater = LayoutInflater.from(context);
			Collator collator = Collator.getInstance();

			data.forEach((i, j) -> {
				boolean include = j != null && j > 0;
				if (include) mData.add(new Pair<>(packageNameGenerator.apply(i), j));
			});
			mData.sort((a, b) -> {
				Long countA = a.second;
				Long countB = b.second;
				int x = countA == null || countB == null ? 0 : countA.compareTo(countB);
				if (x != 0) return -x;
				CharSequence displayA = getAppNameForPkgName(a.first);
				CharSequence displayB = getAppNameForPkgName(b.first);
				return collator.compare(displayA, displayB);
			});
		}

		@NonNull
		@Override
		public DashboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.appitem, parent, false);
			return new DashboardViewHolder(view);
		}

		@Override
		public int getItemCount() {
			return mData.size();
		}

		public Pair<String, Long> getItem(int i) {
			return mData.get(i);
		}

		@Override
		public void onBindViewHolder(@NonNull DashboardViewHolder holder, int position) {
			Pair<String, Long> i = getItem(position);
			holder.apply(i);
		}

		public class DashboardViewHolder extends RecyclerView.ViewHolder {
			private final AppCompatImageView appIcon;
			private final AppCompatTextView appName;
			private final AppCompatTextView subtitle;

			public DashboardViewHolder(@NonNull View itemView) {
				super(itemView);
				this.appIcon = itemView.findViewById(R.id.appIcon);
				this.appName = itemView.findViewById(R.id.appName2);
				this.subtitle = itemView.findViewById(R.id.pkgName);
				AppCompatImageButton actionButton = new AppCompatImageButton(itemView.getContext());
				actionButton.setImageDrawable(AppCompatResources.getDrawable(
						itemView.getContext(), R.drawable.ic_focus_mode));
				actionButton.setBackground(null);
				MaterialCheckBox checkBox = itemView.findViewById(R.id.isChecked);
				ViewGroup parent = (ViewGroup) checkBox.getParent();
				int idx = parent.indexOfChild(checkBox);
				parent.removeView(checkBox);
				//parent.addView(actionButton, idx);
			}

			public void apply(Pair<String, Long> info) {
				appIcon.setImageDrawable(getAppIconForPkgName(info.first));
				appName.setText(getAppNameForPkgName(info.first));
				subtitle.setText(subtitleGenerator.apply(info.first, info.second));
			}
		}
	}

	public String getAppNameForPkgName(String tag) {
		return appNames.computeIfAbsent(tag, packageName -> {
			PackageManager pm = getPackageManager();
			try {
				ApplicationInfo i = pm.getApplicationInfo(packageName, 0);
				return pm.getApplicationLabel(i).toString();
			} catch (PackageManager.NameNotFoundException e) {
				return packageName;
			}
		});
	}

	public Drawable getAppIconForPkgName(String tag) {
		return appIcons.computeIfAbsent(tag, packageName -> {
			PackageManager pm = getPackageManager();
			try {
				return pm.getApplicationIcon(packageName);
			} catch (PackageManager.NameNotFoundException e) {
				return AppCompatResources.getDrawable(DashboardActivity.this, android.R.drawable.sym_def_app_icon);
			}
		});
	}

	public int getAttrColor(int attr) {
		TypedValue typedValue = new TypedValue();
		Resources.Theme theme = getTheme();
		theme.resolveAttribute(attr, typedValue, true);
		@ColorInt int color = typedValue.data;
		return color;
	}

	public int getTextSize(int size) {
		TypedValue typedValue = new TypedValue();
		getTheme().resolveAttribute(size, typedValue, true);
		int[] textSizeAttr = new int[] { android.R.attr.textSize };
		TypedArray a = obtainStyledAttributes(typedValue.data, textSizeAttr);
		int textSize = a.getDimensionPixelSize(0, -1);
		a.recycle();
		return textSize;
	}

	public enum WhatStat {
		SCREEN_TIME("usage", "usage_", false, (ctx) -> (packageName, count) -> ctx.getResources().getQuantityString(R.plurals.break_mins, count.intValue(), count.intValue())),
		NOTIFICATIONS("notif", "notif_", true, (ctx) -> (packageName, count) -> ctx.getString(R.string.notifications_count, count)),
		UNLOCK("unlock", null, true, (ctx) -> null);

		private final String name, prefix;
		private final boolean remote;
		private final Function<Context, BiFunction<String, Long, String>> subtitleGeneratorGenerator;

		WhatStat(String name, String prefix, boolean remote, Function<Context, BiFunction<String, Long, String>> subtitleGeneratorGenerator) {
			this.name = name;
			this.prefix = prefix;
			this.remote = remote;
			this.subtitleGeneratorGenerator = subtitleGeneratorGenerator;
		}

		public String getName() {
			return name;
		}

		public String getPrefix() {
			return prefix;
		}

		public boolean isRemote() {
			return remote;
		}

		public BiFunction<String, Long, String> getSubtitleGenerator(Context context) {
			return subtitleGeneratorGenerator.apply(context);
		}
	}

	// "Today"/"This week"/etc or locale-sensible date
	public @NonNull String fancyDate(LocalDateTime start) {
		if (ExactTime.INSTANCE.plus(LocalDateTime.now(), TimeDimension.values()[whenValue], -1).isBefore(start) && !start.isAfter(LocalDateTime.now())) {
			return thisStrings[whenValue];
		}
		if (whenValue == TimeDimension.HOUR.ordinal()) {
			return SimpleDateFormat.getDateTimeInstance().format(new Date(start.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L));
		}
		return SimpleDateFormat.getDateInstance().format(new Date(start.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000L));
	}
}