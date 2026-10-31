package com.kunvarpreet.standbymode.core.calendar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/**
 * Read-only repository for querying the Android Calendar Provider.
 * Safely handles permissions, background queries, event occurrences, and ContentObserver updates.
 */
class CalendarRepository(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main
) {
    private val tag = "CalendarRepository"

    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher)
    private var activeQueryJob: Job? = null

    private val _uiState = MutableStateFlow<CalendarAgendaUiState>(CalendarAgendaUiState.Loading)
    val uiState: StateFlow<CalendarAgendaUiState> = _uiState.asStateFlow()

    private var contentObserver: ContentObserver? = null
    private var isObserving = false

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Starts observing calendar provider content changes.
     * Guaranteed to register at most one observer instance.
     */
    fun startObserving() {
        if (isObserving) return

        if (!hasCalendarPermission()) {
            _uiState.value = CalendarAgendaUiState.PermissionRequired()
            return
        }

        isObserving = true
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                refresh()
            }
        }
        contentObserver = observer

        try {
            context.contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer
            )
        } catch (e: Exception) {
            Log.w(tag, "Failed to register calendar content observer", e)
        }

        // Trigger initial data load
        refresh()
    }

    /**
     * Stops observing calendar content changes and cleans up resources.
     */
    fun stopObserving() {
        if (!isObserving) return
        isObserving = false

        contentObserver?.let { observer ->
            try {
                context.contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                Log.w(tag, "Error unregistering calendar content observer", e)
            }
        }
        contentObserver = null
        activeQueryJob?.cancel()
        activeQueryJob = null
    }

    /**
     * Triggers a fresh query of the calendar provider.
     */
    fun refresh() {
        if (!hasCalendarPermission()) {
            _uiState.value = CalendarAgendaUiState.PermissionRequired()
            return
        }

        activeQueryJob?.cancel()
        activeQueryJob = scope.launch {
            queryCalendar()
        }
    }

    private suspend fun queryCalendar() {
        if (!hasCalendarPermission()) {
            _uiState.value = CalendarAgendaUiState.PermissionRequired()
            return
        }

        val zoneId = ZoneId.systemDefault()
        val nowMillis = System.currentTimeMillis()
        val today = LocalDate.now(zoneId)
        val currentYearMonth = YearMonth.from(today)
        val locale = Locale.getDefault()

        try {
            val events = withContext(ioDispatcher) {
                queryInstances(currentYearMonth, nowMillis, zoneId)
            }

            val eventDays = CalendarLogic.calculateEventDays(events, currentYearMonth, zoneId)
            val monthData = CalendarLogic.buildMonthData(currentYearMonth, today, locale, eventDays)
            val nextEvent = CalendarLogic.selectNextEvent(events, nowMillis)

            _uiState.value = if (events.isEmpty()) {
                CalendarAgendaUiState.Empty(monthData)
            } else {
                CalendarAgendaUiState.Loaded(monthData, nextEvent)
            }
        } catch (e: SecurityException) {
            Log.w(tag, "Calendar permission revoked during query", e)
            _uiState.value = CalendarAgendaUiState.PermissionRequired()
        } catch (e: Exception) {
            Log.e(tag, "Error querying calendar provider", e)
            val fallbackMonthData = CalendarLogic.buildMonthData(currentYearMonth, today, locale, emptySet())
            _uiState.value = CalendarAgendaUiState.Error(
                message = e.localizedMessage ?: "Failed to read calendar data"
            )
        }
    }

    private fun queryInstances(
        yearMonth: YearMonth,
        nowMillis: Long,
        zoneId: ZoneId
    ): List<CalendarEventItem> {
        val startOfMonthMillis = yearMonth.atDay(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val endOfMonthMillis = yearMonth.atEndOfMonth()
            .atTime(23, 59, 59)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()

        // Cover the month range plus a 30-day upcoming horizon for upcoming events
        val queryStart = minOf(startOfMonthMillis, nowMillis)
        val queryEnd = maxOf(endOfMonthMillis, nowMillis + 30L * 24 * 60 * 60 * 1000)

        val projection = arrayOf(
            CalendarContract.Instances._ID,
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        android.content.ContentUris.appendId(builder, queryStart)
        android.content.ContentUris.appendId(builder, queryEnd)
        val uri = builder.build()

        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC"
        ) ?: return emptyList()

        return cursor.use { c ->
            val eventIdCol = c.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val idCol = c.getColumnIndex(CalendarContract.Instances._ID)
            val titleCol = c.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginCol = c.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endCol = c.getColumnIndex(CalendarContract.Instances.END)
            val allDayCol = c.getColumnIndex(CalendarContract.Instances.ALL_DAY)

            val items = mutableListOf<CalendarEventItem>()
            val maxEvents = 100

            while (c.moveToNext() && items.size < maxEvents) {
                val rawId = if (eventIdCol != -1) c.getLong(eventIdCol) else if (idCol != -1) c.getLong(idCol) else 0L
                val rawTitle = if (titleCol != -1) c.getString(titleCol) else null
                val begin = if (beginCol != -1) c.getLong(beginCol) else 0L
                val end = if (endCol != -1) c.getLong(endCol) else begin
                val allDay = if (allDayCol != -1) c.getInt(allDayCol) == 1 else false

                if (begin > 0L) {
                    items.add(
                        CalendarEventItem(
                            id = rawId,
                            title = rawTitle?.trim().orEmpty(),
                            startMillis = begin,
                            endMillis = if (end >= begin) end else begin,
                            isAllDay = allDay
                        )
                    )
                }
            }
            items
        }
    }
}
