package org.eu.droid_ng.wellbeing.framework;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.provider.Settings;

import org.eu.droid_ng.wellbeing.framework.shim.UserHandlerShim;

public class WellbeingFrameworkServiceImpl extends IWellbeingFrameworkService.Stub {
    private final Context context;

    public WellbeingFrameworkServiceImpl(Context context) {
        this.context = context;
    }

    @Override
    public int versionCode() throws RemoteException {
        return 1;
    }

    @Override
    public void setAirplaneMode(boolean value) throws RemoteException {
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, value ? 1 : 0);
        context.sendBroadcastAsUser(new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                .putExtra("state", value), UserHandlerShim.ALL);
    }
}
