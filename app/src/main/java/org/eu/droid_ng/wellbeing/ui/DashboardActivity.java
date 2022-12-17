package org.eu.droid_ng.wellbeing.ui;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;

import org.eu.droid_ng.wellbeing.R;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dashboard);
		setSupportActionBar(findViewById(R.id.topbar));
		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);

		PieChart pie = (PieChart) findViewById(R.id.chart);
		List<PieEntry> entries = new ArrayList<>();
		entries.add(new PieEntry(18.5f, "App1"));
		entries.add(new PieEntry(26.7f, "App2"));
		entries.add(new PieEntry(24.0f, "App3"));
		entries.add(new PieEntry(30.8f, "Other"));
		PieDataSet set = new PieDataSet(entries, "Top app usage");
		PieData data = new PieData(set);
		pie.setData(data);
		pie.invalidate(); // refresh
	}
}