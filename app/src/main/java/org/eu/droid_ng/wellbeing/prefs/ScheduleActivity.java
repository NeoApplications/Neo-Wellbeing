package org.eu.droid_ng.wellbeing.prefs;

import static org.eu.droid_ng.wellbeing.lib.BugUtils.BUG;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.TimeChargerTriggerCondition;
import org.eu.droid_ng.wellbeing.lib.Trigger;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.util.List;

public class ScheduleActivity extends AppCompatActivity {

	String type;
	List<Trigger> data;
	LinearLayout cardHost;
	View noCardNotification;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		Intent intent = getIntent();
		type = null;
		if (intent != null && intent.hasExtra("type")) {
			type = intent.getStringExtra("type");
		}
		if (type == null) {
			Log.e("ScheduleActivity", "intent or type is null");
			finish();
			return;
		}
		if (intent.hasExtra("name")) {
			setTitle(intent.getStringExtra("name"));
		}
		setContentView(R.layout.activity_schedule);
		cardHost = findViewById(R.id.cardHost);
		noCardNotification = findViewById(R.id.noCardNotification);

		WellbeingService tw = WellbeingService.get();
		data = tw.getTriggersForId(type);
		updateUi();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.schedule_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		if (item.getItemId() == R.id.addSchedule) {
			data.add(new TimeChargerTriggerCondition(type, String.valueOf(System.currentTimeMillis()), true, 7, 0, 18, 0, new boolean[]{true, true, true, true, true, true, true}, false, false));
			updateUi();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateUi() {
		cardHost.removeAllViews();

		if (data.size() < 1) {
			cardHost.setVisibility(View.GONE);
			noCardNotification.setVisibility(View.VISIBLE);
		} else {
			for (Trigger e : data) {
				if (e instanceof TimeChargerTriggerCondition) {
					ScheduleCardView scv = new ScheduleCardView(this);
					scv.setTimeData((TimeChargerTriggerCondition) e);
					scv.setOnValuesChangedCallback(iid -> {
						data.replaceAll(v -> {
							if (v.getIid().equals(iid))
								return scv.getTimeData();
							return v;
						});
						updateUi();
						updateServiceStatus();
					});
					scv.setOnDeleteCardCallback(iid -> {
						data.removeIf(v -> v.getIid().equals(iid));
						updateUi();
						updateServiceStatus();
					});
					cardHost.addView(scv);
				} else {
					BUG("Cannot display " + e.getClass().getCanonicalName());
				}
			}
			cardHost.setVisibility(View.VISIBLE);
			noCardNotification.setVisibility(View.GONE);
		}
	}

	private void updateServiceStatus() {
		WellbeingService tw = WellbeingService.get();
		tw.setTriggersForId(type, data.toArray(new Trigger[0]));
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}