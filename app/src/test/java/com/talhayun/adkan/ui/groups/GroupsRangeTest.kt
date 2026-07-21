package com.talhayun.adkan.ui.groups

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class GroupsRangeTest {

    @Test
    fun `today is not locked and has no upsell message`() {
        assertEquals(false, GroupsRange.TODAY.locked)
        assertNull(GroupsRange.TODAY.upsellMessage())
    }

    @Test
    fun `week is locked and has a non-null Hebrew upsell message`() {
        assertEquals(true, GroupsRange.WEEK.locked)
        assertNotNull(GroupsRange.WEEK.upsellMessage())
        assertEquals("צפייה בנתוני השבוע דורשת מנוי פרימיום", GroupsRange.WEEK.upsellMessage())
    }
}
