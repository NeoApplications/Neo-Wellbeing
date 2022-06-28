package org.eu.droid_ng.wellbeing;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;

import java.util.HashSet;

public class ManualSuspendActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		setContentView(R.layout.activity_manual_suspend);
		WellbeingStateClient client = new WellbeingStateClient(this);
		Button suspendbtn = findViewById(R.id.suspendbtn);
		Button unsuspendbtn = findViewById(R.id.desuspendbtn);
		RecyclerView pkgList = findViewById(R.id.pkgList);
		final PackageRecyclerViewAdapter a;
		pkgList.setAdapter(
				a = new PackageRecyclerViewAdapter(this,
						getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA),
						"manual_suspend"));
		suspendbtn.setOnClickListener(v -> client.doBindService(b -> b.state.manualSuspend(null), false, true, false));
		unsuspendbtn.setOnClickListener(v -> client.doBindService(b -> b.state.manualUnsuspend(a.prefs.getStringSet("manual_suspend", new HashSet<>()).toArray(new String[0]), false)));
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}

}