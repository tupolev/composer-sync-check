package net.kodesoft.composersynccheck

import net.kodesoft.composersynccheck.services.OutOfSyncNotificationPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OutOfSyncNotificationPolicyTest {

    @Test
    fun `manual status check notifies even when ignored and fingerprint is unchanged`() {
        val decision = OutOfSyncNotificationPolicy.decide(
            source = OutOfSyncNotificationPolicy.SOURCE_MANUAL_STATUS_CHECK,
            showNotificationBalloon = true,
            ignoredForSession = true,
            lastNotificationFingerprint = "same",
            currentFingerprint = "same",
            periodicNotificationsShown = 5,
        )

        assertTrue(decision.shouldNotify)
        assertEquals(5, decision.periodicNotificationsShown)
    }

    @Test
    fun `periodic notifications are shown for first two out of sync checks`() {
        val first = OutOfSyncNotificationPolicy.decide(
            source = OutOfSyncNotificationPolicy.SOURCE_PERIODIC,
            showNotificationBalloon = true,
            ignoredForSession = false,
            lastNotificationFingerprint = "a",
            currentFingerprint = "a",
            periodicNotificationsShown = 0,
        )
        val second = OutOfSyncNotificationPolicy.decide(
            source = OutOfSyncNotificationPolicy.SOURCE_PERIODIC,
            showNotificationBalloon = true,
            ignoredForSession = false,
            lastNotificationFingerprint = "a",
            currentFingerprint = "a",
            periodicNotificationsShown = first.periodicNotificationsShown,
        )

        assertTrue(first.shouldNotify)
        assertEquals(1, first.periodicNotificationsShown)
        assertTrue(second.shouldNotify)
        assertEquals(2, second.periodicNotificationsShown)
    }

    @Test
    fun `periodic notifications are skipped from third occurrence in session`() {
        val decision = OutOfSyncNotificationPolicy.decide(
            source = OutOfSyncNotificationPolicy.SOURCE_PERIODIC,
            showNotificationBalloon = true,
            ignoredForSession = false,
            lastNotificationFingerprint = "a",
            currentFingerprint = "a",
            periodicNotificationsShown = 2,
        )

        assertFalse(decision.shouldNotify)
        assertEquals(3, decision.periodicNotificationsShown)
    }

    @Test
    fun `non-periodic automatic notifications are skipped when ignored for session`() {
        val decision = OutOfSyncNotificationPolicy.decide(
            source = "composer-files-changed",
            showNotificationBalloon = true,
            ignoredForSession = true,
            lastNotificationFingerprint = "a",
            currentFingerprint = "b",
            periodicNotificationsShown = 0,
        )

        assertFalse(decision.shouldNotify)
    }

    @Test
    fun `non-periodic automatic notifications are deduplicated by fingerprint`() {
        val decision = OutOfSyncNotificationPolicy.decide(
            source = "git-composer-lock-changed",
            showNotificationBalloon = true,
            ignoredForSession = false,
            lastNotificationFingerprint = "same",
            currentFingerprint = "same",
            periodicNotificationsShown = 0,
        )

        assertFalse(decision.shouldNotify)
    }

    @Test
    fun `setting disabled suppresses all notifications`() {
        val decision = OutOfSyncNotificationPolicy.decide(
            source = OutOfSyncNotificationPolicy.SOURCE_MANUAL_STATUS_CHECK,
            showNotificationBalloon = false,
            ignoredForSession = false,
            lastNotificationFingerprint = null,
            currentFingerprint = "x",
            periodicNotificationsShown = 0,
        )

        assertFalse(decision.shouldNotify)
        assertEquals(0, decision.periodicNotificationsShown)
    }
}
