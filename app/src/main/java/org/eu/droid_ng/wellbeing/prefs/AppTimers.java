package org.eu.droid_ng.wellbeing.prefs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.checkbox.MaterialCheckBox;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.Utils;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.text.Collator;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class AppTimers extends AppCompatActivity {

	private WellbeingService ati;
	private Handler h;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		h = new Handler(getMainLooper());
		ati = WellbeingService.get();
		setContentView(R.layout.activity_app_timers);
		setSupportActionBar(findViewById(R.id.topbar));
		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);
		RecyclerView r = findViewById(R.id.appTimerPkgs);
		new Thread(() -> {
			AppTimersRecyclerViewAdapter a = new AppTimersRecyclerViewAdapter(this,
					ati.getInstalledApplications(PackageManager.GET_META_DATA));
			h.post(() -> {
				findViewById(R.id.appTimerLoading).setVisibility(View.GONE);
				r.setAdapter(a);
				r.setVisibility(View.VISIBLE);
			});
		}).start();
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}

	public class AppTimersRecyclerViewAdapter extends RecyclerView.Adapter<AppTimersRecyclerViewAdapter.AppTimerViewHolder> {
		private final LayoutInflater inflater;
		private final List<ApplicationInfo> mData;
		private final PackageManager pm;
		public final SharedPreferences prefs;
		public final Map<String, Integer> enabledMap = new HashMap<>();

		public AppTimersRecyclerViewAdapter(Context context, List<ApplicationInfo> mData) {
			this.inflater = LayoutInflater.from(context);
			this.pm = context.getPackageManager();
			prefs = context.getSharedPreferences("appTimers", 0);
			prefs.getAll().forEach((k, v) -> {
				if (!(v instanceof Integer)) {
					Log.e("OpenWellbeing", "Failed to parse " + k);
					return;
				}
				Integer v2 = (Integer) v;
				enabledMap.put(k, v2);
			});
			Collator collator = Collator.getInstance();
			// Sort alphabetically by display name
			Comparator<ApplicationInfo> nc = (a, b) -> {
				Duration durationA = Utils.getTimeUsed(ati.usm, a.packageName);
				Duration durationB = Utils.getTimeUsed(ati.usm, b.packageName);
				int x = durationA.compareTo(durationB);
				if (x != 0) return -x;
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
				// Filter out system apps without launcher icon and Default Launcher
				boolean isUser = (i.flags & (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) < 1;
				return !Utils.blackListedPackages.contains(i.packageName) && (isUser || hasLauncherIcon.contains(i.packageName));
			}).sorted((a, b) -> {
				// Enabled goes first
				boolean hasA = !Objects.equals(enabledMap.getOrDefault(a.packageName, 0), 0);
				boolean hasB = !Objects.equals(enabledMap.getOrDefault(b.packageName, 0), 0);
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
		public AppTimerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = inflater.inflate(R.layout.appitem, parent, false);
			return new AppTimerViewHolder(view);
		}

		@Override
		public int getItemCount() {
			return mData.size();
		}

		public ApplicationInfo getItem(int i) {
			return mData.get(i);
		}

		@Override
		public void onBindViewHolder(@NonNull AppTimerViewHolder holder, int position) {
			ApplicationInfo i = getItem(position);
			int mins = prefs.getInt(i.packageName, 0);
			holder.apply(i, mins);
		}

		public class AppTimerViewHolder extends RecyclerView.ViewHolder {
			private final ViewGroup container;
			private final AppCompatImageView appIcon;
			private final AppCompatTextView appName;
			private final AppCompatTextView appTimerInfo;
			private final AppCompatImageButton actionButton;

			public AppTimerViewHolder(@NonNull View itemView) {
				super(itemView);
				this.container = itemView.findViewById(R.id.container);
				this.appIcon = itemView.findViewById(R.id.appIcon);
				this.appName = itemView.findViewById(R.id.appName2);
				this.appTimerInfo = itemView.findViewById(R.id.pkgName);
				actionButton = new AppCompatImageButton(itemView.getContext());
				actionButton.setImageDrawable(AppCompatResources.getDrawable(
						itemView.getContext(), R.drawable.ic_focus_mode));
				actionButton.setBackground(null);
				MaterialCheckBox checkBox = itemView.findViewById(R.id.isChecked);
				ViewGroup parent = (ViewGroup) checkBox.getParent();
				int idx = parent.indexOfChild(checkBox);
				parent.removeView(checkBox);
				parent.addView(actionButton, idx);
			}

			public void apply(ApplicationInfo info, int mins) {
				final boolean restricted = Utils.restrictedPackages.contains(info.packageName);
				appIcon.setImageDrawable(getAppIconForPkgName(info.packageName));
				appName.setText(getAppNameForPkgName(info.packageName));
				applyText(mins, Math.toIntExact(Utils.getTimeUsed(ati.usm, info.packageName).toMinutes()));
				actionButton.setEnabled(!restricted);
				container.setOnClickListener(view -> {
					if (restricted) return;
					Integer realmins = enabledMap.getOrDefault(info.packageName, 0);
					if (realmins == null) return;
					NumberPicker numberPicker = new NumberPicker(AppTimers.this);
					numberPicker.setMinValue(0);
					numberPicker.setMaxValue(9999); //i mean why not
					numberPicker.setValue(realmins);
					new AlertDialog.Builder(AppTimers.this)
							.setTitle(pm.getApplicationLabel(info))
							.setView(numberPicker)
							.setNegativeButton(R.string.cancel, (d,i) -> d.dismiss())
							.setPositiveButton(R.string.ok, (d,i) -> {
								updateMins(info.packageName, realmins, numberPicker.getValue());
								d.dismiss();
							})
							.show();
				});
			}

			private void updateMins(String pkgName, int oldmins, int mins) {
				enabledMap.put(pkgName, mins);
				prefs.edit().putInt(pkgName, mins).apply();
				applyText(mins, Math.toIntExact(Utils.getTimeUsed(ati.usm, pkgName).toMinutes()));
				new Thread(() -> {
					Utils.clearUsageStatsCache(ati.usm, pm, WellbeingService.get().getPmd(), true);
					h.post(() -> {
						applyText(mins, Math.toIntExact(Utils.getTimeUsed(ati.usm, pkgName).toMinutes()));
						ati.onUpdateAppTimerPreference(pkgName, Duration.ofMinutes(oldmins));
					});
				}).start();
			}

			private void applyText(int mins, int mins2) {
				appTimerInfo.setText(itemView.getContext().getString(R.string.desc_container, (mins == 0 ? itemView.getContext().getString(R.string.no_timer) : itemView.getContext().getResources().getQuantityString(R.plurals.break_mins, mins, mins)), (itemView.getContext().getResources().getQuantityString(R.plurals.break_mins, mins2, mins2))));
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
				return AppCompatResources.getDrawable(AppTimers.this, android.R.drawable.sym_def_app_icon);
			}
		});
	}
	public final HashMap<String, Drawable> appIcons = new HashMap<>();
	public final HashMap<String, String> appNames = new HashMap<>();

}