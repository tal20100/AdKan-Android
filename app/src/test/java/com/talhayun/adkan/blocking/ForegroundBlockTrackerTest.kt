package com.talhayun.adkan.blocking

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ForegroundBlockTrackerTest {

    private lateinit var tracker: ForegroundBlockTracker

    @Before
    fun setUp() {
        tracker = ForegroundBlockTracker()
    }

    @Test
    fun `first call for a package returns true`() {
        assertTrue(tracker.shouldLaunchBlockScreen("com.instagram.android"))
    }

    @Test
    fun `repeated calls for the same package return false after the first`() {
        assertTrue(tracker.shouldLaunchBlockScreen("com.instagram.android"))
        assertFalse(tracker.shouldLaunchBlockScreen("com.instagram.android"))
        assertFalse(tracker.shouldLaunchBlockScreen("com.instagram.android"))
    }

    @Test
    fun `a different package returns true again`() {
        assertTrue(tracker.shouldLaunchBlockScreen("com.instagram.android"))
        assertTrue(tracker.shouldLaunchBlockScreen("com.tiktok.android"))
    }

    @Test
    fun `reset allows the same package to re-trigger`() {
        assertTrue(tracker.shouldLaunchBlockScreen("com.instagram.android"))
        assertFalse(tracker.shouldLaunchBlockScreen("com.instagram.android"))
        tracker.reset()
        assertTrue(tracker.shouldLaunchBlockScreen("com.instagram.android"))
    }
}
