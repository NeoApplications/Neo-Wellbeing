package org.eu.droid_ng.wellbeing;

import static org.eu.droid_ng.wellbeing.GlobalWellbeingState.breakTimeOptions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.Arrays;

public class TakeBreakDialogActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		WellbeingStateClient client = new WellbeingStateClient(this);
		client.doBindService(boundService -> {
			String[] optionsS = Arrays.stream(breakTimeOptions).mapToObj(i -> getResources().getQuantityString(R.plurals.break_mins, i, i)).toArray(String[]::new);
			ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, optionsS) {
				@NonNull
				@Override
				public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
					View v = super.getView(position, convertView, parent);
					v.setOnClickListener(view -> {
						boundService.state.takeBreak(breakTimeOptions[position]);
						TakeBreakDialogActivity.this.finish();
					});
					return v;
				}
			};
			ListView lv = new ListView(this);
			lv.setAdapter(a);
			setContentView(lv);
		});
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeAsUpIndicator(R.drawable.ic_baseline_close_24);
	}

	@Override
	public boolean onNavigateUp() {
		finish();
		return true;
	}
}