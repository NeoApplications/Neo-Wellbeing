package org.eu.droid_ng.wellbeing.framework;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.Settings;

import org.eu.droid_ng.wellbeing.lib.IWellbeingFrameworkService;

public class WellbeingFrameworkServiceImpl extends IWellbeingFrameworkService.Stub {
    private UserHandle UserHandle_ALL;
    private final Context context;

    @SuppressWarnings("JavaReflectionMemberAccess")
    public WellbeingFrameworkServiceImpl(Context context) {
        this.context = context;
        try {
            UserHandle_ALL = (UserHandle) UserHandle.class
                    .getDeclaredField("ALL").get(null);
        } catch (Exception ignored) {}
    }

    @Override
    public int versionCode() throws RemoteException {
        return 1;
    }

    @Override
    public void setAirplaneMode(boolean value) throws RemoteException {
        Settings.Global.putInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, value ? 1 : 0);
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED)
                .putExtra("state", value);
        if (UserHandle_ALL != null) {
            context.sendBroadcastAsUser(intent, UserHandle_ALL);
        } else {
            context.sendBroadcast(intent);
        }
    }
}
