package org.eu.droid_ng.wellbeing;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

// Fancy class holding GlobalWellbeingState & a notification
public class WellbeingStateHost extends Service {
	private static final String CHANNEL_ID = "service_notif";
	public GlobalWellbeingState state;

	// Unique Identification Number for the Notification.
	private final int NOTIFICATION = 325563;

	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class LocalBinder extends Binder {
		WellbeingStateHost getService() {
			return WellbeingStateHost.this;
		}
	}

	@Override
	public void onCreate() {
		state = new GlobalWellbeingState();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		NotificationManager notificationManager = getSystemService(NotificationManager.class);
		if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
			CharSequence name = getString(R.string.channel_name);
			String description = getString(R.string.channel_description);
			int importance = NotificationManager.IMPORTANCE_LOW;
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
			channel.setDescription(description);
			notificationManager.createNotificationChannel(channel);
		}

		// TODO: make this more dynamic
		CharSequence text = "Service running";

		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent pendingIntent =
				PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

		Notification notification = new Notification.Builder(this, CHANNEL_ID)
				.setSmallIcon(R.drawable.ic_stat_name)  // the status icon
				.setTicker(text)  // the status text
				.setWhen(System.currentTimeMillis())  // the time stamp
				.setContentTitle("Local service")  // the label of the entry
				.setContentText(text)  // the contents of the entry
				.setContentIntent(pendingIntent)  // The intent to send when the entry is clicked
				.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
				.build();
		// end

		// Notification ID cannot be 0.
		startForeground(NOTIFICATION, notification);

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		state.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();
}
