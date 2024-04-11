package org.eu.droid_ng.wellbeing.shared

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import org.eu.droid_ng.wellbeing.framework.IWellbeingFrameworkService
import java.time.LocalDateTime
import java.time.ZoneId

class WellbeingFrameworkClient(
	private val context: Context,
	private val wellbeingService: ConnectionCallback
) : IWellbeingFrameworkService {
	private val serviceConnection: ServiceConnection
	private var wellbeingFrameworkService: IWellbeingFrameworkService? = null
	private var binder: IBinder? = null
	private var versionCode = 0
	private var initial = true

	init {
		serviceConnection = object : ServiceConnection {
			override fun onServiceConnected(name: ComponentName, service: IBinder) {
				wellbeingFrameworkService = IWellbeingFrameworkService.Stub.asInterface(
					service.also {
						binder = it
					})
				try {
					versionCode = wellbeingFrameworkService!!.versionCode()
				} catch (e: Exception) {
					Log.e("WellbeingFrameworkService", "Failed to get framework version", e)
					invalidateConnection()
					context.unbindService(this)
				}
				if (binder != null || initial) {
					notifyWellbeingService()
				}
			}

			override fun onServiceDisconnected(name: ComponentName) {
				invalidateConnection()
				if (versionCode > -2) HANDLER.post { tryConnect() }
			}

			override fun onBindingDied(name: ComponentName) {
				invalidateConnection()
			}

			override fun onNullBinding(name: ComponentName) {
				invalidateConnection()
			}
		}
	}

	private fun invalidateConnection() {
		wellbeingFrameworkService = DEFAULT
		versionCode = 0
		binder = null
		wellbeingService.onWellbeingFrameworkDisconnected()
	}

	private fun notifyWellbeingService() {
		initial.let {
			initial = false
			wellbeingService.onWellbeingFrameworkConnected(it)
		}
	}

	fun tryConnect() {
		if (versionCode() < 0) return
		if (binder == null || !(binder!!.isBinderAlive && binder!!.pingBinder())) {
			versionCode = -1
			try {
				context.bindService(
					FRAMEWORK_SERVICE_INTENT, serviceConnection,
					Context.BIND_NOT_FOREGROUND or Context.BIND_ALLOW_OOM_MANAGEMENT or Context.BIND_WAIVE_PRIORITY or Context.BIND_ABOVE_CLIENT
				)
			} catch (e: Exception) {
				Log.e("WellbeingFrameworkService", "Failed to bind framework service", e)
				if (versionCode == -1) {
					versionCode = 0
					if (initial) {
						notifyWellbeingService()
					}
				}
			}
		}
	}

	fun tryDisconnect() {
		if (versionCode() < 1) return
		if (binder != null && binder!!.isBinderAlive && binder!!.pingBinder()) {
			versionCode = -2
			context.unbindService(serviceConnection)
		}
	}

	// since 1
	override fun versionCode(): Int {
		if (binder != null && !binder!!.isBinderAlive) {
			invalidateConnection()
		}
		return versionCode
	}

	// since 1
	@Throws(RemoteException::class)
	override fun setAirplaneMode(value: Boolean) {
		if (versionCode() < 1) return
		wellbeingFrameworkService!!.setAirplaneMode(value)
	}

	// since 2
	@Throws(RemoteException::class)
	override fun onNotificationPosted(packageName: String) {
		if (versionCode() < 2) return
		wellbeingFrameworkService!!.onNotificationPosted(packageName)
	}

	// since 2
	@Throws(RemoteException::class)
	override fun getEventCount(type: String, dimension: Int, from: Long, to: Long): Long {
		if (versionCode() < 2) return -1L
		return wellbeingFrameworkService!!.getEventCount(type, dimension, from, to)
	}

	// since 2
	@Throws(RemoteException::class)
	fun getEventCount(type: String, dimension: TimeDimension, from: LocalDateTime, to: LocalDateTime): Long {
		return getEventCount(type, dimension.ordinal, from.atZone(ZoneId.systemDefault()).toEpochSecond(), to.atZone(ZoneId.systemDefault()).toEpochSecond())
	}

	// since 2
	@Throws(RemoteException::class)
	override fun getTypesForPrefix(prefix: String, dimension: Int, from: Long, to: Long): Map<Any?, Any?> {
		if (versionCode() < 2) return hashMapOf()
		return wellbeingFrameworkService!!.getTypesForPrefix(prefix, dimension, from, to)
	}

	// since 2
	@Throws(RemoteException::class)
	fun getTypesForPrefix(prefix: String, dimension: TimeDimension, from: LocalDateTime, to: LocalDateTime): Map<String, Long> {
		return getTypesForPrefix(prefix, dimension.ordinal, from.atZone(ZoneId.systemDefault()).toEpochSecond(), to.atZone(ZoneId.systemDefault()).toEpochSecond())
			.mapKeys { entry -> entry.key as String }.mapValues { entry -> entry.value as Long }
	}

	override fun asBinder(): IBinder {
		return binder!!
	}

	companion object {
		private val HANDLER = Handler(Looper.getMainLooper())
		private val FRAMEWORK_SERVICE_INTENT =
			Intent("org.eu.droid_ng.wellbeing.framework.FRAMEWORK_SERVICE")
				.setPackage("org.eu.droid_ng.wellbeing.framework")
		private var DEFAULT: IWellbeingFrameworkService? = null

		init {
			IWellbeingFrameworkService.Stub.setDefaultImpl(
				IWellbeingFrameworkService.Default().also { DEFAULT = it })
		}
	}

	interface ConnectionCallback {
		fun onWellbeingFrameworkConnected(initial: Boolean)
		fun onWellbeingFrameworkDisconnected()
	}
}