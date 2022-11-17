package org.eu.droid_ng.wellbeing.prefs;

import static org.eu.droid_ng.wellbeing.lib.BugUtils.BUG;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.TimeChargerTriggerCondition;
import org.eu.droid_ng.wellbeing.lib.Trigger;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.util.List;

public class ScheduleActivity extends AppCompatActivity {

	String type;
	List<Trigger> data;
	LinearLayoutCompat cardHost;
	AppCompatTextView noCardNotification;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		setContentView(R.layout.activity_schedule);
		setSupportActionBar(findViewById(R.id.topbar));
		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);
		if (intent.hasExtra("name")) {
			actionBar.setTitle(intent.getStringExtra("name"));
		}
		findViewById(R.id.floating_action_button).setOnClickListener(v -> {
			data.add(new TimeChargerTriggerCondition(type, String.valueOf(System.currentTimeMillis()), true, 7, 0, 18, 0, new boolean[]{true, true, true, true, true, true, true}, false, false));
			updateUi();
			updateServiceStatus();
		});
		cardHost = findViewById(R.id.cardHost);
		noCardNotification = new AppCompatTextView(this);
		noCardNotification.setText(R.string.add_schedule_info);

		WellbeingService tw = WellbeingService.get();
		data = tw.getTriggersForId(type);
		updateUi();
	}

	private void updateUi() {
		cardHost.removeAllViews();

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
				LinearLayoutCompat.LayoutParams m = new LinearLayoutCompat.LayoutParams(scv.getLayoutParams());
				m.setMargins(0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()), 0, 0);
				scv.setLayoutParams(m);
			} else {
				BUG("Cannot display " + e.getClass().getCanonicalName());
			}
		}
		if (data.size() < 1) {
			cardHost.addView(noCardNotification);
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