package com.talhayun.adkan.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileTest {

    @Test
    fun `default profile has an empty name and the default emoji`() {
        val profile = Profile.default()
        assertEquals("", profile.displayName)
        assertEquals("😎", profile.avatarEmoji)
    }

    @Test
    fun `a profile with a real name is not equal to the default`() {
        val profile = Profile(displayName = "טל", avatarEmoji = "🐸")
        assert(profile != Profile.default())
        assertEquals("טל", profile.displayName)
        assertEquals("🐸", profile.avatarEmoji)
    }
}
