package org.eu.droid_ng.wellbeing;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class GlobalWellbeingState {

	private final Context context;
	private final Handler handler;
	private final PackageManagerDelegate packageManagerDelegate;

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


	public GlobalWellbeingState(Context context) {
		this.context = context;
		handler = new Handler(context.getMainLooper());
		packageManagerDelegate = new PackageManagerDelegate(context.getPackageManager());
	}

	public void onDestroy(Context c) {
	}

	// Logic starts here

	public void onManuallyUnsuspended(String packageName) {
		Toast.makeText(context, "Manually unsuspended: " + packageName + " " + reasonMap.getOrDefault(packageName, GlobalWellbeingState.REASON.REASON_UNKNOWN), Toast.LENGTH_LONG).show();
	}

	public void takeBreak(int forMinutes) {
		Toast.makeText(context, "Break start", Toast.LENGTH_LONG).show();
		int forMs = forMinutes * 60 * 1000;
		focusModeUnsuspend();
		focusModeBreak = true;
		handler.postDelayed(() -> {
			Toast.makeText(context, "Break end", Toast.LENGTH_LONG).show();
			focusModeBreak = false;
			focusModeSuspend();
		}, forMs);
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
