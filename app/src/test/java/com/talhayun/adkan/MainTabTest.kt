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
    fun `each tab has a non-blank Hebrew label and emoji`() {
        MainTab.entries.forEach { tab ->
            assert(tab.label.isNotBlank()) { "${tab.name} has a blank label" }
            assert(tab.emoji.isNotBlank()) { "${tab.name} has a blank emoji" }
        }
    }
}
