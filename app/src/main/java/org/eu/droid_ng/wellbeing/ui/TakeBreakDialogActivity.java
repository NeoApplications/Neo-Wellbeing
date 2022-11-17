package org.eu.droid_ng.wellbeing.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ListView;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.WellbeingService;

import java.util.Arrays;

public class TakeBreakDialogActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.take_a_break_activity);
		setSupportActionBar(findViewById(R.id.topbar));
		ActionBar actionBar = getSupportActionBar();
		assert actionBar != null;
		actionBar.setDisplayHomeAsUpEnabled(true);

		WellbeingService tw = WellbeingService.get();
		String[] optionsS = Arrays.stream(WellbeingService.breakTimeOptions).mapToObj(i -> getResources().getQuantityString(R.plurals.break_mins, i, i)).toArray(String[]::new);
		ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, optionsS) {
			@NonNull
			@Override
			public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				v.setOnClickListener(view -> {
					tw.takeFocusModeBreak(WellbeingService.breakTimeOptions[position]);
					TakeBreakDialogActivity.this.finish();
				});
				return v;
			}
		};
		ListView lv = findViewById(R.id.listView);
		lv.setAdapter(a);
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}