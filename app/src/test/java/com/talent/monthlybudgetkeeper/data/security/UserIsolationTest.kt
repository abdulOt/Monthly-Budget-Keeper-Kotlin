package com.talent.monthlybudgetkeeper.data.security

import com.talent.monthlybudgetkeeper.data.repository.UserScopedPreferenceKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UserIsolationTest {

    @Test
    fun `ownership validator allows same user`() {
        UserOwnershipValidator.requireOwnedByUser(
            expectedUserId = "user_a",
            actualUserId = "user_a",
            subject = "transactions"
        )
    }

    @Test(expected = IllegalStateException::class)
    fun `ownership validator blocks different user`() {
        UserOwnershipValidator.requireOwnedByUser(
            expectedUserId = "user_a",
            actualUserId = "user_b",
            subject = "transactions"
        )
    }

    @Test
    fun `user scoped preference keys differ between users`() {
        val first = UserScopedPreferenceKeys.stringName("user_a", "notifications_enabled")
        val second = UserScopedPreferenceKeys.stringName("user_b", "notifications_enabled")

        assertNotEquals(first, second)
        assertEquals("user_a_notifications_enabled", first)
        assertEquals("user_b_notifications_enabled", second)
    }
}
