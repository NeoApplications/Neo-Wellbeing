package org.eu.droid_ng.wellbeing;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.widget.Toast;

import java.util.function.Consumer;

// Helper to connect to WellbeingStateHost
public class WellbeingStateClient {
	// Our context
	private final Context context;

	// Don't attempt to unbind from the service unless the client has received some
	// information about the service's state.
	private boolean mShouldUnbind;

	// To invoke the bound service, first make sure that this value
	// is not null.
	private WellbeingStateHost mBoundService;

	// Callback when service is connected
	private Consumer<WellbeingStateHost> callback;

	// Start the service?
	private final boolean maybeStartService;

	private final ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			try {
				mBoundService = ((WellbeingStateHost.LocalBinder) service).getService();
			} catch (ClassCastException ignored) {
				Toast.makeText(context, "Assertion failure (0xAF): Service is in another process. Please report this to the developers!",
						Toast.LENGTH_SHORT).show();
			}

			if (mBoundService != null)
				WellbeingStateClient.this.callback.accept(mBoundService);
		}

		public void onServiceDisconnected(ComponentName className) {
			// Should never happen
			mBoundService = null;
			Toast.makeText(context, "Assertion failure (0xAE): Service disconnected. Please report this to the developers!",
					Toast.LENGTH_SHORT).show();
		}
	};

	public WellbeingStateClient(Context context, boolean maybeStartService) {
		this.context = context;
		this.maybeStartService = maybeStartService;
	}

	public WellbeingStateClient(Context context) {
		this(context, false);
	}

	void doBindService(Consumer<WellbeingStateHost> callback) {
		this.callback = callback;
		if (context.bindService(new Intent(context, WellbeingStateHost.class),
				mConnection, Context.BIND_IMPORTANT)) {
			mShouldUnbind = true;
		} else {
			if (maybeStartService) {
				context.startForegroundService(new Intent(context, WellbeingStateHost.class));
				if (context.bindService(new Intent(context, WellbeingStateHost.class),
						mConnection, Context.BIND_IMPORTANT)) {
					mShouldUnbind = true;
				} else {
					Toast.makeText(context, "Assertion failure (0xAA): Failed to start service. Please report this to the developers!",
							Toast.LENGTH_SHORT).show();
				}
			} else {
				Toast.makeText(context, "Assertion failure (0xAD): Failed to find service. Please report this to the developers!",
						Toast.LENGTH_SHORT).show();
			}
		}
	}

	void doUnbindService() {
		if (mShouldUnbind) {
			// Release information about the service's state.
			context.unbindService(mConnection);
			mShouldUnbind = false;
		}
	}
}
