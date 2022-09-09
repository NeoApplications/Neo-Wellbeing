package org.eu.droid_ng.wellbeing

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.Toast
import org.eu.droid_ng.wellbeing.WellbeingStateHost.LocalBinder
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
    private var callback: Consumer<WellbeingStateHost?>? = null

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
            callback!!.accept(mBoundService)
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
        callback: Consumer<WellbeingStateHost?>,
        canHandleFailure: Boolean,
        maybeStartService: Boolean = false,
        lateNotify: Boolean = false
    ): Boolean {
        this.callback = callback
        if (mBoundService != null) {
            callback.accept(mBoundService)
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

    fun doBindService(callback: Consumer<WellbeingStateHost?>) {
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