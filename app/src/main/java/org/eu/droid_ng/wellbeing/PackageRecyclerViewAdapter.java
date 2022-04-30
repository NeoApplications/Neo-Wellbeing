package org.eu.droid_ng.wellbeing;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PackageRecyclerViewAdapter extends RecyclerView.Adapter<PackageRecyclerViewAdapter.PackageNameViewHolder> {
	private final LayoutInflater inflater;
	private final List<ApplicationInfo> mData;
	private final List<String> enabledArr;
	private final PackageManager pm;
	public final SharedPreferences prefs;
	private final String settingsKey;

	public PackageRecyclerViewAdapter(Context context, List<ApplicationInfo> mData, String settingsKey) {
		this.inflater = LayoutInflater.from(context);
		this.pm = context.getPackageManager();
		prefs = context.getSharedPreferences("appLists", 0);
		this.settingsKey = settingsKey;
		Set<String> focusAppsS = prefs.getStringSet(this.settingsKey, new HashSet<>());
		enabledArr = new ArrayList<>(focusAppsS);
		Collator collator = Collator.getInstance();
		// Sort alphabetically by display name
		Comparator<ApplicationInfo> nc = (a, b) -> {
			CharSequence displayA = pm.getApplicationLabel(a);
			CharSequence displayB = pm.getApplicationLabel(b);
			return collator.compare(displayA, displayB);
		};
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null)
				.addCategory(Intent.CATEGORY_LAUNCHER);

		List<String> hasLauncherIcon = pm.queryIntentActivities(mainIntent, 0).stream().map(a -> a.activityInfo.packageName).collect(Collectors.toList());
		final List<String> blacklist = List.of("com.android.settings", "com.android.dialer", "org.eu.droid_ng.wellbeing");
		this.mData = mData.stream().filter(i -> {
			// Filter out system apps without launcher icon and Settings, Dialer and Wellbeing
			boolean isUser = (i.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) < 1;
			return !blacklist.contains(i.packageName) && (isUser || hasLauncherIcon.contains(i.packageName));
		}).sorted((a, b) -> {
			// Enabled goes first
			boolean hasA = enabledArr.contains(a.packageName);
			boolean hasB = enabledArr.contains(b.packageName);
			if (hasA && hasB)
				return nc.compare(a, b);
			else if (hasA)
				return -1;
			else if (hasB)
				return 1;
			else
				return nc.compare(a, b);
		}).collect(Collectors.toList());
	}

	@NonNull
	@Override
	public PackageNameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = inflater.inflate(R.layout.appitem, parent, false);
		return new PackageNameViewHolder(view);
	}

	@Override
	public int getItemCount() {
		return mData.size();
	}

	public ApplicationInfo getItem(int i) {
		return mData.get(i);
	}

	@Override
	public void onBindViewHolder(@NonNull PackageNameViewHolder holder, int position) {
		ApplicationInfo i = getItem(position);
		holder.apply(i);
	}

	public class PackageNameViewHolder extends RecyclerView.ViewHolder {
		private final View container;
		private final ImageView appIcon;
		private final TextView appName;
		private final TextView pkgName;
		private final CheckBox checkBox;

		public PackageNameViewHolder(@NonNull View itemView) {
			super(itemView);
			this.container = itemView.findViewById(R.id.container);
			this.appIcon = itemView.findViewById(R.id.appIcon);
			this.appName = itemView.findViewById(R.id.appName2);
			this.pkgName = itemView.findViewById(R.id.pkgName);
			this.checkBox = itemView.findViewById(R.id.isChecked);
		}

		@SuppressLint("ApplySharedPref")
		public void apply(ApplicationInfo info) {
			appIcon.setImageDrawable(pm.getApplicationIcon(info));
			appName.setText(pm.getApplicationLabel(info));
			pkgName.setText(info.packageName);
			checkBox.setChecked(enabledArr.contains(info.packageName));
			container.setOnClickListener(view -> {
				boolean enabled = enabledArr.contains(info.packageName);
				enabled = !enabled;
				checkBox.setChecked(enabled);
				if (enabled) {
					enabledArr.add(info.packageName);
				} else {
					enabledArr.remove(info.packageName);
				}
				prefs.edit().putStringSet(settingsKey, new HashSet<>(enabledArr)).commit();
			});
		}
	}
}

