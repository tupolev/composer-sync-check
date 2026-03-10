package net.kodesoft.composersynccheck.services

internal data class OutOfSyncNotificationDecision(
    val shouldNotify: Boolean,
    val periodicNotificationsShown: Int,
)

internal object OutOfSyncNotificationPolicy {
    const val SOURCE_PERIODIC = "periodic"
    const val SOURCE_MANUAL_STATUS_CHECK = "manual-status-check"

    fun decide(
        source: String,
        showNotificationBalloon: Boolean,
        ignoredForSession: Boolean,
        lastNotificationFingerprint: String?,
        currentFingerprint: String,
        periodicNotificationsShown: Int,
    ): OutOfSyncNotificationDecision {
        if (!showNotificationBalloon) {
            return OutOfSyncNotificationDecision(false, periodicNotificationsShown)
        }

        if (source == SOURCE_MANUAL_STATUS_CHECK) {
            return OutOfSyncNotificationDecision(true, periodicNotificationsShown)
        }

        if (source == SOURCE_PERIODIC) {
            val updatedPeriodicNotificationsShown = periodicNotificationsShown + 1
            return OutOfSyncNotificationDecision(
                shouldNotify = updatedPeriodicNotificationsShown <= 2,
                periodicNotificationsShown = updatedPeriodicNotificationsShown,
            )
        }

        if (ignoredForSession) {
            return OutOfSyncNotificationDecision(false, periodicNotificationsShown)
        }

        if (lastNotificationFingerprint == currentFingerprint) {
            return OutOfSyncNotificationDecision(false, periodicNotificationsShown)
        }

        return OutOfSyncNotificationDecision(true, periodicNotificationsShown)
    }
}
