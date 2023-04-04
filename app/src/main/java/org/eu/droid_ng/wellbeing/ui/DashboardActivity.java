package org.eu.droid_ng.wellbeing.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.checkbox.MaterialCheckBox;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.Utils;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;
import org.eu.droid_ng.wellbeing.shared.TimeDimension;

import java.text.Collator;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class DashboardActivity extends AppCompatActivity {

	public final HashMap<String, Drawable> appIcons = new HashMap<>();
	public final HashMap<String, String> appNames = new HashMap<>();
	public Map<String, Long> rawData;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dashboard);
		setSupportActionBar(findViewById(R.id.topbar));
		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);

		PieChart pie = findViewById(R.id.chart);
		List<PieEntry> entries = new ArrayList<>();
		rawData = WellbeingService.get().getRemoteEventStatsByPrefix("notif_", TimeDimension.DAY, LocalDateTime.now().with(LocalTime.MIN), LocalDateTime.now());
		rawData.forEach((tag, count) -> entries.add(new PieEntry(count, getAppNameForPkgName(tag.substring(6)))));
		entries.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
		if (entries.size() > 4) {
			AtomicLong count = new AtomicLong();
			List<PieEntry> others = entries.subList(4, entries.size());
			others.forEach(p -> count.addAndGet((long) p.getValue()));
			others.clear();
			entries.add(new PieEntry(count.get(), "Other"));
		}
		PieDataSet set = new PieDataSet(entries, "");
		set.setColors(ColorTemplate.JOYFUL_COLORS);
		PieData data = new PieData(set);
		pie.setData(data);
		pie.getDescription().setText("Notifications today");

		pie.getData().setValueTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
		pie.setEntryLabelColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
		pie.getDescription().setTextSize(getTextSize(com.google.android.material.R.attr.tabTextAppearance));
		pie.getDescription().setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
		pie.setHoleColor(getAttrColor(com.google.android.material.R.attr.colorSurface));
		pie.getLegend().setTextColor(getAttrColor(com.google.android.material.R.attr.colorOnSurface));
		pie.getLegend().setWordWrapEnabled(true);
		pie.invalidate(); // refresh

		RecyclerView r = findViewById(R.id.dashboardPkgs);
		new Thread(() -> {
			DashboardRecyclerViewAdapter a = new DashboardRecyclerViewAdapter(this,
					WellbeingService.get().getInstalledApplications(PackageManager.GET_META_DATA));
			runOnUiThread(() -> {
				findViewById(R.id.dashboardLoading).setVisibility(View.GONE);
				findViewById(R.id.dashboardContainer).setVisibility(View.VISIBLE);
				r.setAdapter(a);
			});
		}).start();
	}

	public class DashboardRecyclerViewAdapter extends RecyclerView.Adapter<DashboardRecyclerViewAdapter.DashboardViewHolder> {
		private final LayoutInflater inflater;
		private final List<ApplicationInfo> mData;

		public DashboardRecyclerViewAdapter(Context context, List<ApplicationInfo> mData) {
			this.inflater = LayoutInflater.from(context);
			PackageManager pm = context.getPackageManager();
			Collator collator = Collator.getInstance();
			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null)
					.addCategory(Intent.CATEGORY_LAUNCHER);

			// We already force include user apps, so let's only iterate over system apps
			List<String> hasLauncherIcon = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_SYSTEM_ONLY)
					.stream().map(a -> a.activityInfo.packageName).collect(Collectors.toList());
			this.mData = mData.stream().filter(i -> {
				// Filter out system apps without launcher icon and Default Launcher
				boolean isUser = (i.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) < 1;
				Long value = rawData.get("notif_" + i.packageName);
				return !Utils.blackListedPackages.contains(i.packageName) && (isUser || hasLauncherIcon.contains(i.packageName)) && value != null && value > 0;
			}).sorted((a, b) -> {
				Long countA = rawData.get("notif_" + a.packageName);
				Long countB = rawData.get("notif_" + b.packageName);
				int x = countA == null || countB == null ? 0 : countA.compareTo(countB);
				if (x != 0) return -x;
				CharSequence displayA = pm.getApplicationLabel(a);
				CharSequence displayB = pm.getApplicationLabel(b);
				return collator.compare(displayA, displayB);
			}).collect(Collectors.toList());
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

		public ApplicationInfo getItem(int i) {
			return mData.get(i);
		}

		@Override
		public void onBindViewHolder(@NonNull DashboardViewHolder holder, int position) {
			ApplicationInfo i = getItem(position);
			holder.apply(i, rawData.get("notif_" + i.packageName));
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

			public void apply(ApplicationInfo info, Long count) {
				appIcon.setImageDrawable(getAppIconForPkgName(info.packageName));
				appName.setText(getAppNameForPkgName(info.packageName));
				subtitle.setText(getString(R.string.notifications_count, count));
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
}