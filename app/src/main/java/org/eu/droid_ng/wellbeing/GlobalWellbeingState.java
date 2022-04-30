package org.eu.droid_ng.wellbeing;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class GlobalWellbeingState {

	public static final String INTENT_ACTION_TAKE_BREAK = "org.eu.droid_ng.wellbeing.TAKE_BREAK";
	public static final String INTENT_ACTION_QUIT_BREAK = "org.eu.droid_ng.wellbeing.QUIT_BREAK";
	public static final String INTENT_ACTION_QUIT_FOCUS = "org.eu.droid_ng.wellbeing.QUIT_FOCUS";
	public static final String INTENT_ACTION_UNSUSPEND_ALL = "org.eu.droid_ng.wellbeing.UNSUSPEND_ALL";
	public static final int[] breakTimeOptions = new int[] { 1, 3, 5, 10, 15 };

	private final Context context;
	private final Handler handler;
	private final WellbeingStateHost service;
	private final PackageManagerDelegate packageManagerDelegate;

	// set to -1 for "ask every time"
	public int notificationBreakTime = -1;
	// set to -1 for "ask every time"
	public int dialogBreakTime = -1;
	// if app was suspended by focus mode, and gets unsuspended in dialog, unsuspend all apps?
	public boolean focusModeAllApps = false;

	// if app was suspended manually, and gets unsuspended in dialog, unsuspend all apps?
	public boolean manualUnsuspendDialogAllApps = false;
	// use dialog for manual unsuspend? (activity allows to choose between unsuspend all and unsuspend only this)
	public boolean manualUnsuspendDialog = false;

	enum REASON {
		REASON_MANUALLY,
		REASON_UNKNOWN,
		REASON_FOCUS_MODE,
		REASON_FOCUS_MODE_BREAK
	}
	public Map<String, REASON> reasonMap = new HashMap<>();

	enum SERVICE_TYPE {
		TYPE_MANUALLY,
		TYPE_UNKNOWN,
		TYPE_FOCUS_MODE
	}
	public SERVICE_TYPE type = SERVICE_TYPE.TYPE_UNKNOWN;
	public List<Runnable> stateChangeCallbacks = new ArrayList<>();

	public Set<String> focusModePackages;
	public Set<String> manualSuspendPackages;
	public boolean focusModeBreak = false;
	private final Runnable takeBreakEndRunnable;
	private String[] manualSuspendPkgList = new String[0];


	public GlobalWellbeingState(Context context, WellbeingStateHost service) {
		this.context = context;
		this.service = service;
		handler = new Handler(context.getMainLooper());
		packageManagerDelegate = new PackageManagerDelegate(context.getPackageManager());
		takeBreakEndRunnable = () -> {
			if (!focusModeBreak)
				return;
			focusModeBreak = false;
			focusModeSuspend(false);
			makeFocusModeNotification();
		};
		SharedPreferences prefs = context.getSharedPreferences("service", 0);
		SharedPreferences prefs2 = context.getSharedPreferences("appLists", 0);
		focusModePackages = prefs2.getStringSet("focus_mode", new HashSet<>());
		manualSuspendPackages = prefs2.getStringSet("manual_suspend", new HashSet<>());
		notificationBreakTime = Integer.parseInt(prefs.getString("focus_notification", String.valueOf(notificationBreakTime)));
		dialogBreakTime = Integer.parseInt(prefs.getString("focus_dialog", String.valueOf(dialogBreakTime)));
		manualUnsuspendDialog = prefs.getBoolean("manual_dialog", manualUnsuspendDialog);
		manualUnsuspendDialogAllApps = prefs.getBoolean("manual_all", manualUnsuspendDialogAllApps);
		focusModeAllApps = prefs.getBoolean("focus_all", focusModeAllApps);
	}

	public void onDestroy() {
	}

	public void onManuallyUnsuspended(@NonNull String packageName) {
		REASON suspendReason = reasonMap.getOrDefault(packageName, REASON.REASON_UNKNOWN);
		if (suspendReason == null)
			suspendReason = REASON.REASON_UNKNOWN;
		reasonMap.remove(packageName);
		switch (suspendReason) {
			case REASON_FOCUS_MODE:
				takeBreak(focusModeAllApps ? null : new String[] { packageName }, dialogBreakTime);
				break;
			case REASON_MANUALLY:
				if (manualUnsuspendDialogAllApps) {
					manualUnsuspend(null, true);
				}
				// If all manually suspended apps are unsuspended, gracefully quit
				if (reasonMap.keySet().stream().noneMatch(v -> reasonMap.get(v) == REASON.REASON_MANUALLY)) {
					manualUnsuspend(null, true);
					service.stop();
				}
				break;
			case REASON_UNKNOWN:
			default:
				// Do nothing.
				break;
		}
	}

	private void takeBreak(String[] packageNames, int forMinutes) {
		if (packageNames == null) {
			takeBreak(forMinutes);
			return;
		}
		int forMs = forMinutes * 60 * 1000;
		String[] failed = packageManagerDelegate.setPackagesSuspended(packageNames, false, null, null, null);
		for (String packageName : failed) {
			Log.e("OpenWellbeing", "failed to unsuspend " + packageName);
		}
		for (String packageName : packageNames) {
			reasonMap.put(packageName, REASON.REASON_FOCUS_MODE_BREAK);
		}
		handler.postDelayed(() -> focusModeSuspend(packageNames, true), forMs);
	}

	public void takeBreak(int forMinutes) {
		if (focusModeBreak)
			return;
		int forMs = forMinutes * 60 * 1000;
		focusModeUnsuspend(false);
		focusModeBreak = true;
		handler.postDelayed(takeBreakEndRunnable, forMs);
		makeFocusModeBreakNotification();
		onStateChange();
	}

	public void endBreak() {
		handler.removeCallbacks(takeBreakEndRunnable);
		takeBreakEndRunnable.run();
		onStateChange();
	}

	public void takeBreakDialog(Activity activityContext, boolean endActivity, String[] packageNames) {
		String[] optionsS = Arrays.stream(breakTimeOptions).mapToObj(i -> context.getResources().getQuantityString(R.plurals.break_mins, i, i)).toArray(String[]::new);
		AlertDialog.Builder b = new AlertDialog.Builder(activityContext);
		b.setTitle(R.string.focus_mode_break)
				.setNegativeButton(R.string.cancel, (d, i) -> d.dismiss())
				.setItems(optionsS, (dialogInterface, i) -> {
					int breakMins = breakTimeOptions[i];
					takeBreak(packageNames, breakMins);
					if (endActivity) activityContext.finish();
				});
		b.show();
	}

	public void enableFocusMode() {
		if (type != SERVICE_TYPE.TYPE_UNKNOWN)
			return;
		focusModeBreak = false;
		type = SERVICE_TYPE.TYPE_FOCUS_MODE;
		makeFocusModeNotification();
		focusModeSuspend(true);
		onStateChange();
	}

	private void makeFocusModeNotification() {
		service.updateNotification(R.string.focus_mode, R.string.notification_focus_mode, R.drawable.ic_stat_name, new Notification.Action[]{
				notificationBreakTime == -1 ?
				service.buildAction(R.string.focus_mode_break, R.drawable.ic_take_break, new Intent(context, TakeBreakDialogActivity.class), false)
				: service.buildAction(R.string.focus_mode_break, R.drawable.ic_take_break, new Intent(context, NotificationBroadcastReciever.class).setAction(GlobalWellbeingState.INTENT_ACTION_TAKE_BREAK), true),
				service.buildAction(R.string.focus_mode_off, R.drawable.ic_stat_name, new Intent(context, NotificationBroadcastReciever.class).setAction(GlobalWellbeingState.INTENT_ACTION_QUIT_FOCUS), true)
		}, new Intent(context, MainActivity.class));
	}

	private void makeFocusModeBreakNotification() {
		service.updateNotification(R.string.focus_mode, R.string.notification_focus_mode_break, R.drawable.ic_stat_name, new Notification.Action[]{
				service.buildAction(R.string.focus_mode_break_end, R.drawable.ic_take_break, new Intent(context, NotificationBroadcastReciever.class).setAction(GlobalWellbeingState.INTENT_ACTION_QUIT_BREAK), true),
				service.buildAction(R.string.focus_mode_off, R.drawable.ic_stat_name, new Intent(context, NotificationBroadcastReciever.class).setAction(GlobalWellbeingState.INTENT_ACTION_QUIT_FOCUS), true)
		}, new Intent(context, MainActivity.class));
	}

	public void disableFocusMode() {
		if (focusModeBreak) {
			focusModeBreak = false;
		} else {
			focusModeUnsuspend(true);
		}
		type = SERVICE_TYPE.TYPE_UNKNOWN;
		onStateChange();
		service.updateDefaultNotification();
		service.stop();
	}

	private void focusModeSuspend(String[] process, boolean enable) {
		for (String packageName : process) {
			if (enable)
				reasonMap.put(packageName, REASON.REASON_FOCUS_MODE);
			else if (reasonMap.get(packageName) == REASON.REASON_FOCUS_MODE_BREAK)
				continue;
			String[] failed = new String[0];
			try {
				failed = packageManagerDelegate.setPackagesSuspended(new String[] { packageName }, true, null, null, new PackageManagerDelegate.SuspendDialogInfo.Builder()
						.setTitle(R.string.focus_mode_enabled)
						.setMessage(context.getString(R.string.focus_mode_dialog, context.getPackageManager().getApplicationLabel(context.getPackageManager().getApplicationInfo(packageName, 0))))
						.setNeutralButtonText(dialogBreakTime == -1 ? R.string.dialog_btn_settings : context.getResources().getIdentifier("break_dialog_" + dialogBreakTime, "string", context.getPackageName()))
						.setNeutralButtonAction(dialogBreakTime == -1 ? PackageManagerDelegate.SuspendDialogInfo.BUTTON_ACTION_MORE_DETAILS : PackageManagerDelegate.SuspendDialogInfo.BUTTON_ACTION_UNSUSPEND)
						.setIcon(R.drawable.ic_baseline_app_blocking_24).build());
			} catch (PackageManager.NameNotFoundException e) {
				e.printStackTrace();
			}
			for (String f : failed) {
				Log.e("OpenWellbeing", "failed to suspend " + f);
			}
		}
	}

	private void focusModeSuspend(boolean enable) {
		String[] process = focusModePackages.stream().distinct().toArray(String[]::new);
		focusModeSuspend(process, enable);
	}

	private void focusModeUnsuspend(boolean disable) {
		// All packages in focus mode setting + all packages marked as suspended due to FOCUS_MODE (catch now-removed apps & other bugs)
		String[] process = Stream.concat(focusModePackages.stream(), reasonMap.keySet().stream().filter(packageName -> reasonMap.get(packageName) == REASON.REASON_FOCUS_MODE)).distinct().toArray(String[]::new);
		String[] failed = packageManagerDelegate.setPackagesSuspended(process, false, null, null, null);
		for (String packageName : failed) {
			Log.e("OpenWellbeing", "failed to unsuspend " + packageName);
		}
		if (disable) {
			for (String packageName : process) {
				reasonMap.remove(packageName);
			}
		}
	}

	public void manualSuspend(@Nullable String[] pkgNames) {
		if (type != SERVICE_TYPE.TYPE_UNKNOWN && type != SERVICE_TYPE.TYPE_MANUALLY)
			return;
		String[] packageNames = manualSuspendPackages.stream().distinct().toArray(String[]::new);
		if (pkgNames != null) {
			packageNames = Stream.concat(Arrays.stream(pkgNames), Arrays.stream(packageNames)).distinct().toArray(String[]::new);
		}
		manualSuspendPkgList = Stream.concat(Arrays.stream(manualSuspendPkgList), Arrays.stream(packageNames)).distinct().toArray(String[]::new);
		if (type == SERVICE_TYPE.TYPE_UNKNOWN) {
			type = SERVICE_TYPE.TYPE_MANUALLY;
			service.updateNotification(R.string.notification_title, R.string.notification_manual, R.drawable.ic_stat_name, new Notification.Action[]{
					service.buildAction(R.string.unsuspend_all, R.drawable.ic_stat_name, new Intent(context, NotificationBroadcastReciever.class).setAction(GlobalWellbeingState.INTENT_ACTION_UNSUSPEND_ALL), true)
			}, new Intent(context, MainActivity.class));
		}
		String[] failed = packageManagerDelegate.setPackagesSuspended(packageNames, true, null, null, new PackageManagerDelegate.SuspendDialogInfo.Builder()
				.setTitle(R.string.dialog_title)
				.setMessage(R.string.dialog_message)
				.setNeutralButtonText(manualUnsuspendDialog ? R.string.unsuspend : R.string.dialog_btn_settings)
				.setNeutralButtonAction(manualUnsuspendDialog ? PackageManagerDelegate.SuspendDialogInfo.BUTTON_ACTION_UNSUSPEND : PackageManagerDelegate.SuspendDialogInfo.BUTTON_ACTION_MORE_DETAILS)
				.setIcon(R.drawable.ic_baseline_app_blocking_24).build());
		for (String packageName : failed) {
			Log.e("OpenWellbeing", "failed to suspend " + packageName);
		}
		for (String packageName : packageNames) {
			reasonMap.put(packageName, REASON.REASON_MANUALLY);
		}
		onStateChange();
	}

	public void manualUnsuspend(@Nullable String[] pkgNames, boolean wantQuit) {
		assert pkgNames != null || !wantQuit;
		String[] packageNames = new String[0];
		if (wantQuit) {
			packageNames = Stream.concat(manualSuspendPackages.stream(), Arrays.stream(manualSuspendPkgList)).distinct().toArray(String[]::new);
		}
		if (pkgNames != null) {
			packageNames = Stream.concat(Arrays.stream(pkgNames), Arrays.stream(packageNames)).distinct().toArray(String[]::new);
		}
		String[] failed = packageManagerDelegate.setPackagesSuspended(packageNames, false, null, null, null);
		for (String packageName : failed) {
			Log.e("OpenWellbeing", "failed to unsuspend " + packageName);
		}
		for (String packageName : packageNames) {
			reasonMap.remove(packageName);
		}
		boolean haveNoMatch = reasonMap.keySet().stream().noneMatch(v -> reasonMap.get(v) == REASON.REASON_MANUALLY);
		assert !wantQuit || haveNoMatch;
		if (haveNoMatch) {
			type = SERVICE_TYPE.TYPE_UNKNOWN;
		}
		onStateChange();
		if (haveNoMatch) {
			service.updateDefaultNotification();
			service.stop();
		}
	}

	private void onStateChange() {
		for (Runnable stateChangeCallback : stateChangeCallbacks)
			stateChangeCallback.run();
	}
}
