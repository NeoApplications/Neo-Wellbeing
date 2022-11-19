package org.eu.droid_ng.wellbeing.lib;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

public class WellbeingFrameworkService implements IWellbeingFrameworkService {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private static final Intent FRAMEWORK_SERVICE_INTENT =
            new Intent("org.eu.droid_ng.wellbeing.lib.FRAMEWORK_SERVICE")
                    .setPackage("org.eu.droid_ng.wellbeing.framework");
    private static final IWellbeingFrameworkService DEFAULT;
    static {
        Stub.setDefaultImpl(DEFAULT = new Default());
    }
    private final Context context;
    private final WellbeingService wellbeingService;
    private final boolean system;
    private final ServiceConnection serviceConnection;
    private IWellbeingFrameworkService wellbeingFrameworkService;
    private IBinder binder;
    private int versionCode;
    private boolean initial = true;

    WellbeingFrameworkService(Context context, WellbeingService wellbeingService, boolean system) {
        this.context = context;
        this.wellbeingService = wellbeingService;
        this.system = system;
        this.serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (service == null) {
                    onNullBinding(name);
                    return;
                }
                wellbeingFrameworkService = Stub.asInterface(
                        WellbeingFrameworkService.this.binder = service);
                try {
                    versionCode = wellbeingFrameworkService.versionCode();
                } catch (Exception e) {
                    Log.e("WellbeingFrameworkService", "Failed to get framework version", e);
                    invalidateConnection();
                    context.unbindService(this);
                }
                if (binder != null || initial) {
                    notifyWellbeingService();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                invalidateConnection();
                HANDLER.post(() -> tryConnect());
            }

            @Override
            public void onBindingDied(ComponentName name) {
                invalidateConnection();
                context.unbindService(this);
            }

            @Override
            public void onNullBinding(ComponentName name) {
                invalidateConnection();
                context.unbindService(this);
                if (initial) {
                    notifyWellbeingService();
                }
            }
        };
    }

    private void invalidateConnection() {
        wellbeingFrameworkService = DEFAULT;
        versionCode = 0;
        binder = null;
    }

    private void notifyWellbeingService() {
        boolean initial = this.initial;
        this.initial = false;
        this.wellbeingService.onWellbeingFrameworkConnected(initial);
    }

    public void tryConnect() {
        if (versionCode == -1) return;
        if (binder == null || !(binder.isBinderAlive() && binder.pingBinder())) {
            versionCode = -1;
            try {
                context.bindService(FRAMEWORK_SERVICE_INTENT, this.serviceConnection,
                        Context.BIND_AUTO_CREATE | Context.BIND_INCLUDE_CAPABILITIES);
            } catch (Exception e) {
                Log.e("WellbeingFrameworkService", "Failed to bind framework service", e);
                if (versionCode == -1) {
                    versionCode = 0;
                    if (initial) {
                        notifyWellbeingService();
                    }
                }
            }
        }
    }

    @Override
    public int versionCode() {
        if (binder != null && !binder.isBinderAlive()) {
            invalidateConnection();
        }
        // Allow partial emulation of the setAirplaneMode call if connection failed.
        if (versionCode == 0 && system && !initial) {
            return 1;
        }
        return versionCode;
    }

    @Override
    public void setAirplaneMode(boolean value) throws RemoteException {
        if (this.versionCode < 1) {
            if (system && !initial) {
                // This causes SEVERE issues with UI and stuff because we
                // cannot notify system/apps of the new airplane mode state.
                Settings.Global.putInt(context.getContentResolver(),
                        Settings.Global.AIRPLANE_MODE_ON, value ? 1 : 0);
            }
            return;
        }
        wellbeingFrameworkService.setAirplaneMode(value);
    }

    @Override
    public IBinder asBinder() {
        return this.binder;
    }
}
