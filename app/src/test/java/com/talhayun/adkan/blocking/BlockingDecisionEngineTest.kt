package com.talhayun.adkan.blocking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockingDecisionEngineTest {

    private val instagram = "com.instagram.android"
    private val calculator = "com.android.calculator2"

    @Test
    fun `app not in the selected set is never blocked`() {
        val result = BlockingDecisionEngine.shouldBlock(
            packageName = calculator,
            selectedApps = setOf(instagram),
            blockingEnabled = true,
            alwaysBlockEnabled = false,
            todayForegroundMinutes = 999,
        )
        assertFalse(result)
    }

    @Test
    fun `blocking disabled and always-block off means never blocked, even over threshold`() {
        val result = BlockingDecisionEngine.shouldBlock(
            packageName = instagram,
            selectedApps = setOf(instagram),
            blockingEnabled = false,
            alwaysBlockEnabled = false,
            todayForegroundMinutes = 999,
        )
        assertFalse(result)
    }

    @Test
    fun `selected app under the threshold is not blocked`() {
        val result = BlockingDecisionEngine.shouldBlock(
            packageName = instagram,
            selectedApps = setOf(instagram),
            blockingEnabled = true,
            alwaysBlockEnabled = false,
            todayForegroundMinutes = 29,
            thresholdMinutes = 30,
        )
        assertFalse(result)
    }

    @Test
    fun `selected app at or over the threshold is blocked when blocking is enabled`() {
        val result = BlockingDecisionEngine.shouldBlock(
            packageName = instagram,
            selectedApps = setOf(instagram),
            blockingEnabled = true,
            alwaysBlockEnabled = false,
            todayForegroundMinutes = 30,
            thresholdMinutes = 30,
        )
        assertTrue(result)
    }

    @Test
    fun `always-block enabled blocks a selected app regardless of minutes`() {
        val result = BlockingDecisionEngine.shouldBlock(
            packageName = instagram,
            selectedApps = setOf(instagram),
            blockingEnabled = false,
            alwaysBlockEnabled = true,
            todayForegroundMinutes = 0,
        )
        assertTrue(result)
    }
}
