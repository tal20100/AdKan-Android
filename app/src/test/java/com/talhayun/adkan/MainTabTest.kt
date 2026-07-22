package com.talhayun.adkan

import org.junit.Assert.assertEquals
import org.junit.Test

class MainTabTest {

    @Test
    fun `entries are in Home, Friends, Groups, Blocking, Settings order`() {
        assertEquals(
            listOf(MainTab.HOME, MainTab.FRIENDS, MainTab.GROUPS, MainTab.BLOCKING, MainTab.SETTINGS),
            MainTab.entries.toList(),
        )
    }

    @Test
    fun `each tab has a non-blank Hebrew label and a distinct icon`() {
        MainTab.entries.forEach { tab ->
            assert(tab.label.isNotBlank()) { "${tab.name} has a blank label" }
        }
        assertEquals(
            "each tab must have a distinct icon (no accidental duplicate assignment)",
            MainTab.entries.size,
            MainTab.entries.map { it.icon }.distinct().size,
        )
    }
}
