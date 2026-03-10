package net.kodesoft.composersynccheck.notifications

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import net.kodesoft.composersynccheck.ComposerSyncCheckBundle

class ComposerSyncNotificationService(private val project: Project) {

    private val group = NotificationGroupManager.getInstance().getNotificationGroup("Composer Sync Check")
    private val notificationIcon = IconLoader.getIcon("/META-INF/composerSyncToolWindow.svg", javaClass)

    fun notifyOutOfSync(onRunInstall: () -> Unit, onIgnoreSession: () -> Unit) {
        val notification = group.createNotification(
            ComposerSyncCheckBundle.message("notification.title"),
            ComposerSyncCheckBundle.message("notification.message"),
            NotificationType.WARNING,
        )
        notification.setIcon(notificationIcon)

        notification.addAction(
            NotificationAction.createSimple(ComposerSyncCheckBundle.message("notification.action.run")) {
                notification.expire()
                onRunInstall()
            },
        )
        notification.addAction(
            NotificationAction.createSimple(ComposerSyncCheckBundle.message("notification.action.ignore")) {
                notification.expire()
                onIgnoreSession()
            },
        )

        notification.notify(project)
    }
}
