package org.eu.droid_ng.wellbeing;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class GlobalWellbeingState {

	public static final String INTENT_ACTION_TAKE_BREAK = "org.eu.droid_ng.wellbeing.TAKE_BREAK";
	public static final String INTENT_ACTION_QUIT_BREAK = "org.eu.droid_ng.wellbeing.QUIT_BREAK";
	public static final String INTENT_ACTION_QUIT_FOCUS = "org.eu.droid_ng.wellbeing.QUIT_FOCUS";
	public static final int[] breakTimeOptions = new int[] { 1, 3, 5, 10, 15 };

	private final Context context;
	private final Handler handler;
	private final WellbeingStateHost service;
	private final PackageManagerDelegate packageManagerDelegate;

	// set to -1 for "ask every time"
	public int notificationBreakTime = 5;

	enum REASON {
		REASON_MANUALLY,
		REASON_UNKNOWN,
		REASON_FOCUS_MODE
	}
	public Map<String, REASON> reasonMap = new HashMap<>();

	enum SERVICE_TYPE {
		TYPE_MANUALLY,
		TYPE_UNKNOWN,
		TYPE_FOCUS_MODE
	}
	public SERVICE_TYPE type = SERVICE_TYPE.TYPE_UNKNOWN;

	public List<String> focusModePackages = new ArrayList<>(List.of("org.lineageos.jelly", "org.lineageos.eleven"));
	private boolean focusModeBreak = false;
	private final Runnable takeBreakEndRunnable;


	public GlobalWellbeingState(Context context, WellbeingStateHost service) {
		this.context = context;
		this.service = service;
		handler = new Handler(context.getMainLooper());
		packageManagerDelegate = new PackageManagerDelegate(context.getPackageManager());
		takeBreakEndRunnable = () -> {
			if (!focusModeBreak)
				return;
			focusModeBreak = false;
			focusModeSuspend();
			makeFocusModeNotification();
		};
	}

	public void onDestroy() {
	}

	public void onManuallyUnsuspended(String packageName) {
		Toast.makeText(context, "Manually unsuspended: " + packageName + " " + reasonMap.getOrDefault(packageName, GlobalWellbeingState.REASON.REASON_UNKNOWN), Toast.LENGTH_LONG).show();
	}

	public void takeBreak(int forMinutes) {
		if (focusModeBreak)
			return;
		int forMs = forMinutes * 60 * 1000;
		focusModeUnsuspend();
		focusModeBreak = true;
		handler.postDelayed(takeBreakEndRunnable, forMs);
		makeFocusModeBreakNotification();
	}

	public void endBreak() {
		handler.removeCallbacks(takeBreakEndRunnable);
		takeBreakEndRunnable.run();
	}

	public void takeBreakDialog(Activity activityContext, boolean endActivity) {
		String[] optionsS = Arrays.stream(breakTimeOptions).mapToObj(i -> context.getResources().getQuantityString(R.plurals.break_mins, i, i)).toArray(String[]::new);
		AlertDialog.Builder b = new AlertDialog.Builder(activityContext);
		b.setTitle(R.string.focus_mode_break)
				.setNegativeButton(R.string.cancel, (d, i) -> d.dismiss())
				.setItems(optionsS, (dialogInterface, i) -> {
					int breakMins = breakTimeOptions[i];
					takeBreak(breakMins);
					if (endActivity) activityContext.finish();
				});
		b.show();
	}

	public void enableFocusMode() {
		focusModeBreak = false;
		type = SERVICE_TYPE.TYPE_FOCUS_MODE;
		makeFocusModeNotification();
		focusModeSuspend();
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
			focusModeUnsuspend();
		}
		type = SERVICE_TYPE.TYPE_UNKNOWN;
		service.updateDefaultNotification();
	}

	public void focusModeSuspend() {
		String[] process = focusModePackages.stream().distinct().toArray(String[]::new);
		String[] failed = packageManagerDelegate.setPackagesSuspended(process, true, null, null, new PackageManagerDelegate.SuspendDialogInfo.Builder()
				.setTitle(R.string.dialog_title)
				.setMessage(R.string.dialog_message)
				.setNeutralButtonText(R.string.dialog_btn_settings)
				.setNeutralButtonAction(PackageManagerDelegate.SuspendDialogInfo.BUTTON_ACTION_MORE_DETAILS)
				.setIcon(R.drawable.ic_baseline_app_blocking_24).build());
		for (String packageName : failed) {
			Log.e("OpenWellbeing", "failed to suspend " + packageName);
		}
		for (String packageName : process) {
			reasonMap.put(packageName, REASON.REASON_FOCUS_MODE);
		}
	}

	public void focusModeUnsuspend() {
		// All packages in focus mode setting + all packages marked as suspended due to FOCUS_MODE (catch now-removed apps & other bugs)
		String[] process = Stream.concat(focusModePackages.stream(), reasonMap.keySet().stream().filter(packageName -> reasonMap.get(packageName) == REASON.REASON_FOCUS_MODE)).distinct().toArray(String[]::new);
		String[] failed = packageManagerDelegate.setPackagesSuspended(process, false, null, null, null);
		for (String packageName : failed) {
			Log.e("OpenWellbeing", "failed to unsuspend " + packageName);
		}
		for (String packageName : process) {
			reasonMap.remove(packageName);
		}
	}
}
