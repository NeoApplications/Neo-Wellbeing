package org.eu.droid_ng.wellbeing.prefs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.Utils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PackageRecyclerViewAdapter extends RecyclerView.Adapter<PackageRecyclerViewAdapter.PackageNameViewHolder> {
	private final Context mContext;
	private final LayoutInflater inflater;
	private final List<ApplicationInfo> mData;
	private final List<String> enabledArr;
	private final PackageManager pm;
	public final SharedPreferences prefs;
	private final String settingsKey;
	private final Consumer<String> callback;

	public PackageRecyclerViewAdapter(Context context, List<ApplicationInfo> mData, String settingsKey, @Nullable Consumer<String> callback) {
		this.mContext = context;
		this.inflater = LayoutInflater.from(mContext);
		this.pm = mContext.getPackageManager();
		this.callback = callback;
		prefs = mContext.getSharedPreferences("appLists", 0);
		this.settingsKey = settingsKey;
		Set<String> focusAppsS = prefs.getStringSet(this.settingsKey, new HashSet<>());
		enabledArr = new ArrayList<>(focusAppsS);
		Collator collator = Collator.getInstance();
		// Sort alphabetically by display name
		Comparator<ApplicationInfo> nc = (a, b) -> {
			CharSequence displayA = getAppNameForPkgName(a.packageName);
			CharSequence displayB = getAppNameForPkgName(b.packageName);
			return collator.compare(displayA, displayB);
		};
		Intent mainIntent = new Intent(Intent.ACTION_MAIN, null)
				.addCategory(Intent.CATEGORY_LAUNCHER);

		// We already force include user apps, so let's only iterate over system apps
		List<String> hasLauncherIcon = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_SYSTEM_ONLY)
				.stream().map(a -> a.activityInfo.packageName).collect(Collectors.toList());
		this.mData = mData.stream().filter(i -> {
			// Filter out system apps without launcher icon and Settings, Dialer and Wellbeing
			boolean isUser = (i.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) < 1;
			return !Utils.restrictedPackages.contains(i.packageName) && (isUser || hasLauncherIcon.contains(i.packageName));
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
		holder.apply(i.packageName);
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
		public void apply(String packageName) {
			appIcon.setImageDrawable(getAppIconForPkgName(packageName));
			appName.setText(getAppNameForPkgName(packageName));
			pkgName.setText(packageName);
			checkBox.setChecked(enabledArr.contains(packageName));
			container.setOnClickListener(view -> {
				boolean enabled = enabledArr.contains(packageName);
				enabled = !enabled;
				checkBox.setChecked(enabled);
				if (enabled) {
					enabledArr.add(packageName);
				} else {
					enabledArr.remove(packageName);
				}
				prefs.edit().putStringSet(settingsKey, new HashSet<>(enabledArr)).commit();
				if (callback != null) {
					callback.accept(packageName);
				}
			});
		}
	}


	public String getAppNameForPkgName(String tag) {
		return appNames.computeIfAbsent(tag, packageName -> {
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
			try {
				return pm.getApplicationIcon(packageName);
			} catch (PackageManager.NameNotFoundException e) {
				return AppCompatResources.getDrawable(mContext, android.R.drawable.sym_def_app_icon);
			}
		});
	}
	public final HashMap<String, Drawable> appIcons = new HashMap<>();
	public final HashMap<String, String> appNames = new HashMap<>();

}

