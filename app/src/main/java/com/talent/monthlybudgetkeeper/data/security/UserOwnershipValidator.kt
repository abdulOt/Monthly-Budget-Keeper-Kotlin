package com.talent.monthlybudgetkeeper.data.security

object UserOwnershipValidator {
    fun requireOwnedByUser(
        expectedUserId: String,
        actualUserId: String,
        subject: String
    ) {
        check(actualUserId == expectedUserId) {
            "Blocked a cross-user $subject response for a different account."
        }
    }
}
