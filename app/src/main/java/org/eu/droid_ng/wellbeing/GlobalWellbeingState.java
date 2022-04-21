package org.eu.droid_ng.wellbeing;

import java.util.HashMap;
import java.util.Map;

public class GlobalWellbeingState {

	enum REASON {
		REASON_MANUALLY,
		REASON_UNKNOWN,
		REASON_FOCUS_MODE
	}
	public Map<String, REASON> reasonMap = new HashMap<>();

	public GlobalWellbeingState() {

	}

	public void onDestroy() {

	}
}
