package org.eu.droid_ng.wellbeing.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.eu.droid_ng.wellbeing.R;
import org.eu.droid_ng.wellbeing.lib.State;
import org.eu.droid_ng.wellbeing.lib.TransistentWellbeingState;

import java.util.Arrays;

public class TakeBreakDialogActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TransistentWellbeingState.use(this, tw -> {
			String[] optionsS = Arrays.stream(State.breakTimeOptions).mapToObj(i -> getResources().getQuantityString(R.plurals.break_mins, i, i)).toArray(String[]::new);
			ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, optionsS) {
				@NonNull
				@Override
				public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
					View v = super.getView(position, convertView, parent);
					v.setOnClickListener(view -> {
						tw.takeFocusModeBreak(State.breakTimeOptions[position]);
						TakeBreakDialogActivity.this.finish();
					});
					return v;
				}
			};
			ListView lv = new ListView(this);
			lv.setAdapter(a);
			getActionBar().setDisplayHomeAsUpEnabled(true);
			getActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_close_24);
			setContentView(lv);
		});

	}

	@Override
	public boolean onNavigateUp() {
		finish();
		return true;
	}
}