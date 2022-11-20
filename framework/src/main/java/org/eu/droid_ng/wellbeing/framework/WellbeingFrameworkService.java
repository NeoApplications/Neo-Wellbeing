package org.eu.droid_ng.wellbeing.framework;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class WellbeingFrameworkService extends Service {
    private final WellbeingFrameworkServiceImpl
            wellbeingFrameworkService = new WellbeingFrameworkServiceImpl(this);

    @Override
    public IBinder onBind(Intent intent) {
        if (!"org.eu.droid_ng.wellbeing.framework.FRAMEWORK_SERVICE"
                .equals(intent.getAction())) return null;
        return wellbeingFrameworkService;
    }
}
