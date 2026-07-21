package com.talhayun.adkan.screentime

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

// [SKILL-DECL] Confirmed via developer.android.com UsageStatsManager docs:
// queryAndAggregateUsageStats(beginTime, endTime) returns one aggregated
// UsageStats entry per package for the range (unlike queryUsageStats, which
// can return multiple overlapping buckets) — summing totalTimeInForeground
// across all entries is the standard approach for "total device screen time"
// used by open-source usage-tracking apps, mirroring what iOS's
// ScreenTimeSnapshot conceptually represents (todayMinutes/yesterdayMinutes)
// via DeviceActivity there. Requires PACKAGE_USAGE_STATS (see
// UsageAccessPermission) — callers must check isGranted() first; this object
// doesn't check itself so it stays a pure data reader, not a permission gate.
object ScreenTimeReader {

    /** Total foreground minutes across all apps from midnight today until now. */
    fun todayMinutes(context: Context): Int {
        val start = startOfDay(daysAgo = 0)
        val end = System.currentTimeMillis()
        return aggregateMinutes(context, start, end)
    }

    /** Total foreground minutes across all apps for the full previous calendar day. */
    fun yesterdayMinutes(context: Context): Int {
        val start = startOfDay(daysAgo = 1)
        val end = startOfDay(daysAgo = 0)
        return aggregateMinutes(context, start, end)
    }

    private fun aggregateMinutes(context: Context, start: Long, end: Long): Int {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return 0
        val stats = usageStatsManager.queryAndAggregateUsageStats(start, end)
        val totalMillis = stats.values.sumOf { it.totalTimeInForeground }
        return (totalMillis / 60_000L).toInt()
    }

    private fun startOfDay(daysAgo: Int): Long {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -daysAgo)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
