package org.eu.droid_ng.wellbeing.prefs;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.material.button.MaterialButton;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.util.HashSet;

public class ManualSuspendActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manual_suspend);
		setSupportActionBar(findViewById(R.id.topbar));
		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);
		MaterialButton suspendbtn = findViewById(R.id.suspendbtn);
		MaterialButton unsuspendbtn = findViewById(R.id.desuspendbtn);
		RecyclerView pkgList = findViewById(R.id.pkgList);
		final PackageRecyclerViewAdapter a;
		pkgList.setAdapter(
				a = new PackageRecyclerViewAdapter(this,
						getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA),
						"manual_suspend", null));
		WellbeingService tw = WellbeingService.get();
		suspendbtn.setOnClickListener(v -> tw.manualSuspend(null));
		unsuspendbtn.setOnClickListener(v -> tw.manualUnsuspend(a.prefs.getStringSet("manual_suspend", new HashSet<>()).toArray(new String[0])));
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}

}