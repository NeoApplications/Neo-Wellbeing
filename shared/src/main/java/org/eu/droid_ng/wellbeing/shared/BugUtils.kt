package org.eu.droid_ng.wellbeing.shared

import android.content.Context
import android.util.ArrayMap
import android.util.Log
import android.util.SparseArray
import android.util.SparseLongArray
import java.io.File
import java.nio.charset.Charset
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class BugUtils(private val bugFolder: File, private val pkg: String) {
	companion object {
		private var utils: BugUtils? = null

		fun maybeInit(context: Context) {
			if (utils == null) {
				val f = File(context.cacheDir, "bugutils")
				f.mkdirs()
				utils = BugUtils(f, context.packageName)
			}
		}

		@Suppress("FunctionName")
		fun BUG(message: String) {
			if (utils != null) {
				utils?.onBugAdded(RuntimeException(message), System.currentTimeMillis())
				Log.e("Wellbeing:BugUtils", "BUG \"$message\"")
			} else {
				Log.e("Wellbeing:BugUtils", "had to drop BUG \"$message\"")
			}
		}

		fun get(): BugUtils? {
			return utils
		}

		fun formatDateForRender(epochMillis: Long): String {
			return DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()))
		}
	}

	private fun cleanup() {
		bugFolder.list()?.let {
			val l = it.asList().sorted()
			if (l.size > 20) {
				l.subList(0, l.size - 21).forEach { name ->
					File(bugFolder, name).delete()
				}
			}
		}
	}

	fun onBugAdded(message: Throwable, date: Long) {
		val l = "$pkg\n${Log.getStackTraceString(message)}"
		val o = File(bugFolder, date.toString()).outputStream()
		o.write(l.toByteArray(Charset.defaultCharset()))
		o.close()
		cleanup()
	}

	fun hasBugs(): Boolean {
		return (bugFolder.list()?.size ?: 0) > 0
	}

	fun getBugs(): Map<Long, String> {
		val m = hashMapOf<Long, String>()
		bugFolder.list()?.let {
			val l = it.asList().sorted()
			l.forEach { date ->
				val content = File(bugFolder, date).readText(Charset.defaultCharset())
				m[date.toLong()] = content
			}
		}
		return m
	}
}