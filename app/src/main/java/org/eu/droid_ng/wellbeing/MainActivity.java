package org.eu.droid_ng.wellbeing;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import org.eu.droid_ng.wellbeing.PackageManagerDelegate.SuspendDialogInfo;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		PackageManagerDelegate delegate = new PackageManagerDelegate(getPackageManager());
		findViewById(R.id.button).setOnClickListener(a -> {
			for (String s : delegate.setPackagesSuspended(new String[]{"org.lineageos.jelly","org.lineageos.eleven"},true,null,null,new SuspendDialogInfo.Builder().setTitle(R.string.dialog_title).setMessage(R.string.dialog_message).setNeutralButtonText(R.string.dialog_btn_settings).setNeutralButtonAction(SuspendDialogInfo.BUTTON_ACTION_MORE_DETAILS).setIcon(R.drawable.ic_baseline_app_blocking_24).build())) {
				Log.e("OpenWellbeing", "failed: " + s);
			}
		});
		findViewById(R.id.button2).setOnClickListener(a -> {
			for (String s : delegate.setPackagesSuspended(new String[]{"org.lineageos.jelly","org.lineageos.eleven"},false,null,null,null)) {
				Log.e("OpenWellbeing", "failed: " + s);
			}
		});
	}
}