package org.eu.droid_ng.wellbeing.shared

import android.content.Context
import android.os.Handler
import android.util.Log
import androidx.room.Database
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Entity(primaryKeys = ["date", "unit", "type"])
private data class StatEntry(
	// Unix time because yes
	val date: Long,
	val unit: TimeDimension = TimeDimension.ERROR,
	val type: String,

	val count: Long,
) {
	override fun toString(): String {
		return "StatEntry{date=$date, unit=$unit, count=$count}"
	}
}

enum class TimeDimension {
	YEAR, MONTH, DAY, HOUR, ERROR
}

object ExactTime {
	private fun ofHour(date: LocalDateTime): LocalDateTime {
		return date.withMinute(0).withSecond(0).withNano(0)
	}

	private fun ofDay(date: LocalDateTime): LocalDateTime {
		return ofHour(date.with(LocalTime.MIN))
	}

	private fun ofMonth(date: LocalDateTime): LocalDateTime {
		return ofDay(date.withDayOfMonth(1))
	}

	private fun ofYear(date: LocalDateTime): LocalDateTime {
		return ofMonth(date.withMonth(1))
	}

	fun ofUnit(date: LocalDateTime, dimension: TimeDimension): LocalDateTime {
		return when(dimension) {
			TimeDimension.YEAR -> ofYear(date)
			TimeDimension.MONTH -> ofMonth(date)
			TimeDimension.DAY -> ofDay(date)
			TimeDimension.HOUR -> ofHour(date)
			else -> LocalDateTime.now()
		}
	}

	fun of(date: LocalDateTime, dimension: TimeDimension): Long {
		return ofUnit(date, dimension).atZone(ZoneId.systemDefault()).toEpochSecond()
	}

	fun plus(old: LocalDateTime, dimension: TimeDimension?, count: Int): LocalDateTime {
		when (dimension) {
			TimeDimension.YEAR -> return old.plusYears(count.toLong())
			TimeDimension.MONTH -> return old.plusMonths(count.toLong())
			TimeDimension.DAY -> return old.plusDays(count.toLong())
			TimeDimension.HOUR -> return old.plusHours(count.toLong())
			else -> {}
		}
		throw IllegalArgumentException()
	}

}

@Dao
private abstract class StatDao {
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insert(stat: StatEntry)

	@Update
	abstract fun update(stat: StatEntry)

	@Delete
	abstract fun delete(stat: StatEntry)

	@Query(
		"SELECT MIN(statentry.date) FROM statentry WHERE statentry.type = :type"
	)
	abstract fun getEarliest(type: String): Long

	@Query(
		"SELECT * FROM statentry WHERE statentry.type LIKE :prefix || '%' AND statentry.unit = :unit AND " +
				"statentry.date >= :min AND statentry.date < :max"
	)
	abstract fun findStatsOfPrefixBetween(prefix: String, unit: TimeDimension, min: Long, max: Long): List<StatEntry>

	@Query(
		"SELECT * FROM statentry WHERE statentry.type = :type AND statentry.unit = :unit AND " +
				"statentry.date >= :min AND statentry.date < :max"
	)
	abstract fun findStatsOfTypeBetween(type: String, unit: TimeDimension, min: Long, max: Long): List<StatEntry>

	@Query(
		"SELECT * FROM statentry WHERE statentry.type = :type AND statentry.unit = :unit AND " +
				"statentry.date = :date"
	)
	abstract fun findStatsOfTypeWhere(type: String, unit: TimeDimension, date: Long): List<StatEntry>

	fun insert(type: String, date: LocalDateTime, dimension: TimeDimension, count: Long) {
		insert(StatEntry(ExactTime.of(date, dimension), dimension, type, count))
	}

	fun increment(type: String, date: LocalDateTime, dimension: TimeDimension) {
		val results = findStatsOfTypeWhere(type, dimension, ExactTime.of(date, dimension))
		if (results.size > 1) {
			// Should never happen
			Log.e("WellbeingDatabase", "FATAL, destroying invalid data! results.size > 1")
			Log.e("WellbeingDatabase", results[0].toString())
			for (i in 1..<results.size) {
				Log.e("WellbeingDatabase", results[i].toString())
				delete(results[i])
			}
		}
		if (results.isEmpty()) {
			insert(type, date, dimension, 1)
		} else {
			val oldEntry = results[0]
			update(StatEntry(oldEntry.date, oldEntry.unit, oldEntry.type,oldEntry.count + 1))
		}
	}
}

@Database(entities = [StatEntry::class], version = 1)
private abstract class StatDb : RoomDatabase() {
	abstract fun statDao(): StatDao
}

class Database(context: Context, private val bgHandler: Handler, private val consolidateDelay: Int) {
	private val db = Room.databaseBuilder(
		context,
		StatDb::class.java, "stats"
	).build()
	private val dao = db.statDao()
	private var lastConsolidate = 0L

	fun incrementNow(type: String) {
		while (lastConsolidate == -1L) {
			Thread.sleep(100)
		}
		dao.increment(type, LocalDateTime.now(), TimeDimension.HOUR)
		maybeConsolidate(type)
	}

	fun insert(type: String, date: LocalDateTime, dimension: TimeDimension, count: Long) {
		while (lastConsolidate == -1L) {
			Thread.sleep(100)
		}
		dao.insert(type, date, dimension, count)
		maybeConsolidate(type)
	}

	fun getCountFor(type: String, dimension: TimeDimension, from: LocalDateTime, to: LocalDateTime): Long {
		while (lastConsolidate == -1L) {
			Thread.sleep(100)
		}
		val tfrom = ExactTime.of(from, dimension)
		val tto = ExactTime.of(to, dimension)
		val results = dao.findStatsOfTypeBetween(type, dimension, tfrom, tto)
		if (results.isEmpty()) {
			val newdim = TimeDimension.entries[dimension.ordinal + 1]
			if (newdim == TimeDimension.ERROR) return 0
			return getCountFor(type, newdim, from, to)
		}
		var count = 0L
		results.forEach { count += it.count }
		maybeConsolidate(type)
		return count
	}

	fun getTypesForPrefix(prefix: String, dimension: TimeDimension, from: LocalDateTime, to: LocalDateTime): Map<String, Long> {
		while (lastConsolidate == -1L) {
			Thread.sleep(100)
		}
		val tfrom = ExactTime.of(from, dimension)
		val tto = ExactTime.of(to, dimension)
		val results = dao.findStatsOfPrefixBetween(prefix, dimension, tfrom, tto)
		if (results.isEmpty()) {
			val newdim = TimeDimension.entries[dimension.ordinal + 1]
			if (newdim == TimeDimension.ERROR) return hashMapOf()
			return getTypesForPrefix(prefix, newdim, from, to)
		}
		maybeConsolidate(prefix)
		val fresult = HashMap<String, Long>()
		results.forEach { fresult.merge(it.type, it.count) { old, new -> old + new } }
		return fresult
	}

	private fun maybeConsolidate(type: String) {
		if (consolidateDelay > 0) {
			bgHandler.postDelayed({
				if (lastConsolidate + consolidateDelay > System.currentTimeMillis()) {
					return@postDelayed
				}
				consolidate(type)
			}, consolidateDelay / 2L)
		}
	}

	fun consolidate(type: String, every: Boolean = false) {
		if (lastConsolidate == -1L) return
		lastConsolidate = -1L
		val earliest = LocalDateTime.ofInstant(
			Instant.ofEpochSecond(dao.getEarliest(type)), ZoneId.systemDefault())
		consolidateUnit(type,
			TimeDimension.DAY, { it.minusDays(1) }, false, earliest, LocalDateTime.now().minusDays(7)) // hour -> day. store hourly stats for 7 days
		consolidateUnit(type,
			TimeDimension.MONTH, { it.minusMonths(1) }, false, earliest, LocalDateTime.now().minusMonths(3)) // day -> month. store daily stats for 3 months
		consolidateUnit(type,
			TimeDimension.YEAR, { it.minusYears(1) }, every, earliest, LocalDateTime.now().minusYears(10)) // month -> year. store monthly stats for 10 years
		lastConsolidate = System.currentTimeMillis()
	}

	private fun consolidateUnit(type: String, dimension: TimeDimension, genFrom: (LocalDateTime) -> LocalDateTime, every: Boolean, earliest: LocalDateTime, last: LocalDateTime = LocalDateTime.now()) {
		if (!ExactTime.ofUnit(last, dimension).isAfter(earliest)) return
		val from = genFrom(last)
		val to = ExactTime.of(last, dimension)
		val newdim = TimeDimension.entries[dimension.ordinal + 1]
		if (newdim == TimeDimension.ERROR) return
		val results = dao.findStatsOfTypeBetween(type, newdim, ExactTime.of(from, dimension), to)
		var count = 0L
		results.forEach {
			count += it.count
			dao.delete(it)
		}
		if (count > 0) {
			dao.insert(type, from, dimension, count)
		}
		if (every || results.isNotEmpty()) {
			consolidateUnit(type, dimension, genFrom, every, earliest, from)
		}
	}
}