package org.eu.droid_ng.wellbeing;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.drawable.Icon;
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
		state = new GlobalWellbeingState(getApplicationContext());
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

		int text = R.string.notification_desc;
		int title = R.string.notification_title;
		int icon = R.drawable.ic_stat_name;
		Intent notificationIntent = new Intent(this, MainActivity.class);

		// Notification ID cannot be 0.
		startForeground(NOTIFICATION, buildNotification(title, text, icon, new Notification.Action[]{ }, notificationIntent));

		return START_STICKY;
	}

	public Notification.Action buildAction(int actionText, int actionIcon, Intent actionIntent) {
		PendingIntent pendingIntent =
				PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE);
		return new Notification.Action.Builder(Icon.createWithResource(getApplicationContext(), actionIcon), getText(actionText), pendingIntent)
				.setAuthenticationRequired(true)
				.setAllowGeneratedReplies(false)
				.setContextual(true)
				.build();
	}

	private Notification buildNotification(int title, int text, int icon, Notification.Action[] actions, Intent notificationIntent) {
		PendingIntent pendingIntent =
				PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

		Notification.Builder b = new Notification.Builder(this, CHANNEL_ID)
				.setSmallIcon(icon)  // the status icon
				.setTicker(getText(text))  // the status text
				.setWhen(System.currentTimeMillis())  // the time stamp
				.setContentTitle(getText(title))  // the label of the entry
				.setContentText(getText(text))  // the contents of the entry
				.setContentIntent(pendingIntent)  // The intent to send when the entry is clicked
				.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE) // do not wait with showing the notification
				.setOnlyAlertOnce(true); // dont headsup/bling twice
		for (Notification.Action action : actions) {
			b.addAction(action);
		}
		return b.build();
	}

	public void updateNotification(int title, int text, int icon, Notification.Action[] actions, Intent notificationIntent) {
		getSystemService(NotificationManager.class).notify(NOTIFICATION, buildNotification(title, text, icon, actions, notificationIntent));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		state.onDestroy(getApplicationContext());
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	// This is the object that receives interactions from clients.  See
	// RemoteService for a more complete example.
	private final IBinder mBinder = new LocalBinder();
}
