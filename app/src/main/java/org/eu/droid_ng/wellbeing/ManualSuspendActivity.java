package org.eu.droid_ng.wellbeing;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;

public class ManualSuspendActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_manual_suspend);
		WellbeingStateClient client = new WellbeingStateClient(this);
		Button suspendbtn = findViewById(R.id.suspendbtn);
		Button unsuspendbtn = findViewById(R.id.desuspendbtn);
		RecyclerView pkgList = findViewById(R.id.pkgList);
		pkgList.setAdapter(
				new PackageRecyclerViewAdapter(this,
						getPackageManager().getInstalledApplications(PackageManager.GET_META_DATA),
						"manual_suspend"));
		suspendbtn.setOnClickListener(v -> client.doBindService(b -> b.state.manualSuspend(null)));
		unsuspendbtn.setOnClickListener(v -> client.doBindService(b -> b.state.manualUnsuspend(null)));
	}
}