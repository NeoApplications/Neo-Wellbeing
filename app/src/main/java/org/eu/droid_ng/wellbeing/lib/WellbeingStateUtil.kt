package org.eu.droid_ng.wellbeing.lib

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Icon
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import org.eu.droid_ng.wellbeing.prefs.MainActivity
import org.eu.droid_ng.wellbeing.R
import org.eu.droid_ng.wellbeing.lib.WellbeingStateHost.LocalBinder
import java.util.function.Consumer

// Helper to connect to WellbeingStateHost
class WellbeingStateClient(context: Context) {
    // Our context
    private val context: Context

    // Don't attempt to unbind from the service unless the client has received some
    // information about the service's state.
    private var mShouldUnbind = false

    // To invoke the bound service, first make sure that this value
    // is not null.
    private var mBoundService: WellbeingStateHost? = null

    // Callback when service is connected
    private var callback: Consumer<GlobalWellbeingState?>? = null

    // Connection callback utility
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            mBoundService = try {
                (service as LocalBinder).service
            } catch (ignored: ClassCastException) {
                Toast.makeText(
                    context,
                    "Assertion failure (0xAE): Service is in another process. Please report this to the developers!",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            callback!!.accept(mBoundService!!.state)
        }

        override fun onServiceDisconnected(className: ComponentName) {
            mBoundService = null
        }

        /*override fun onNullBinding(className: ComponentName) {
			Toast.makeText(context, "Assertion failure (0xAF): Service is null. Please report this to the developers!",
					Toast.LENGTH_SHORT).show();
		}*/
    }

    //backward compatibility does what we want, so ignore warning
    @SuppressWarnings("deprecation")
    fun isServiceRunning(): Boolean {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (WellbeingStateHost::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }

    @JvmOverloads
    fun doBindService(
        callback: Consumer<GlobalWellbeingState?>,
        canHandleFailure: Boolean,
        maybeStartService: Boolean = false,
        lateNotify: Boolean = false
    ): Boolean {
        this.callback = callback
        if (mBoundService != null) {
            callback.accept(mBoundService!!.state)
            return true
        }
        return if (isServiceRunning() && context.bindService(
                Intent(context, WellbeingStateHost::class.java),
                mConnection, Context.BIND_IMPORTANT
            )
        ) {
            mShouldUnbind = true
            true
        } else {
            if (maybeStartService) {
                startService(lateNotify)
                if (doBindService(callback, canHandleFailure = true, maybeStartService = false, lateNotify)) {
                    return true
                } else if (!canHandleFailure) {
                    Toast.makeText(
                        context,
                        "Assertion failure (0xAA): Failed to start service. Please report this to the developers!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (!canHandleFailure) {
                Toast.makeText(
                    context,
                    "Assertion failure (0xAD): Failed to find service. Please report this to the developers!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            false
        }
    }

    fun doBindService(callback: Consumer<GlobalWellbeingState?>) {
        doBindService(callback, false)
    }

    fun doUnbindService() {
        if (mShouldUnbind) {
            // Release information about the service's state.
            context.unbindService(mConnection)
            mShouldUnbind = false
        }
    }

    @JvmOverloads
    fun startService(lateNotify: Boolean = false) {
        val i = Intent(context, WellbeingStateHost::class.java)
        i.putExtra("lateNotify", lateNotify)
        context.startForegroundService(i)
    }

    fun killService() {
        context.stopService(Intent(context, WellbeingStateHost::class.java))
    }

    init {
        this.context = context.applicationContext
    }
}

// Fancy class holding GlobalWellbeingState & a notification
class WellbeingStateHost : Service() {
    @JvmField
    var state: GlobalWellbeingState? = null
    private var lateNotify = false

    // Unique Identification Number for the Notification.
    private val NOTIFICATION = 325563
    private val CHANNEL_ID = "service_notif"

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    inner class LocalBinder : Binder() {
        val service: WellbeingStateHost
            get() = this@WellbeingStateHost
    }

    override fun onCreate() {
        state = GlobalWellbeingState(applicationContext, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationManager = getSystemService(
            NotificationManager::class.java
        )
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val name: CharSequence = getString(R.string.channel_name)
            val description = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            notificationManager.createNotificationChannel(channel)
        }
        if (intent != null) {
            lateNotify = intent.getBooleanExtra("lateNotify", lateNotify)
        }
        val n = buildDefaultNotification()

        // Notification ID cannot be 0.
        startForeground(NOTIFICATION, n)
        return START_STICKY
    }

    fun buildAction(
        actionText: Int,
        actionIcon: Int,
        actionIntent: Intent?,
        isBroadcast: Boolean
    ): Notification.Action {
        val pendingIntent = if (isBroadcast) {
            PendingIntent.getBroadcast(this, 0, actionIntent!!, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, actionIntent, PendingIntent.FLAG_IMMUTABLE)
        }
        val builder = Notification.Action.Builder(
            Icon.createWithResource(applicationContext, actionIcon),
            getText(actionText),
            pendingIntent
        )
            .setAllowGeneratedReplies(false).setContextual(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAuthenticationRequired(true)
        }
        return builder.build()
    }

    private fun buildNotification(
        title: Int,
        text: String,
        icon: Int,
        actions: Array<Notification.Action>,
        notificationIntent: Intent
    ): Notification {
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        val b = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(icon) // the status icon
            .setTicker(text) // the status text
            .setWhen(System.currentTimeMillis()) // the time stamp
            .setContentTitle(getText(title)) // the label of the entry
            .setContentText(text) // the contents of the entry
            .setContentIntent(pendingIntent) // The intent to send when the entry is clicked
            .setOnlyAlertOnce(true) // dont headsup/bling twice
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !lateNotify) {
            b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE) // do not wait with showing the notification
        }
        if (lateNotify) lateNotify = false
        for (action in actions) {
            b.addAction(action)
        }
        return b.build()
    }

    private fun buildDefaultNotification(): Notification {
        val text = R.string.notification_desc
        val title = R.string.notification_title
        val icon = R.drawable.ic_stat_name
        val notificationIntent = Intent(this, MainActivity::class.java)
        return buildNotification(title, getString(text), icon, arrayOf(), notificationIntent)
    }

    private fun updateNotification(n: Notification) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION, n)
    }

    fun updateNotification(
        title: Int,
        text: String,
        icon: Int,
        actions: Array<Notification.Action>,
        notificationIntent: Intent
    ) {
        updateNotification(buildNotification(title, text, icon, actions, notificationIntent))
    }

    fun updateNotification(
        title: Int,
        text: Int,
        icon: Int,
        actions: Array<Notification.Action>,
        notificationIntent: Intent
    ) {
        updateNotification(title, getString(text), icon, actions, notificationIntent)
    }

    fun updateDefaultNotification() {
        updateNotification(buildDefaultNotification())
    }

    fun stop() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        state!!.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private val mBinder: IBinder = LocalBinder()
}