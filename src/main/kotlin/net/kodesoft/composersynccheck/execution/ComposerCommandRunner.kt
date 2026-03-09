package net.kodesoft.composersynccheck.execution

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.concurrency.AppExecutorUtil
import net.kodesoft.composersynccheck.ComposerSyncCheckBundle
import net.kodesoft.composersynccheck.toolwindow.ComposerSyncConsoleService
import java.awt.Color
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlin.concurrent.thread
import java.util.concurrent.atomic.AtomicBoolean

class ComposerCommandRunner(
    private val project: Project,
    private val console: ComposerSyncConsoleService,
) {

    private val logger = Logger.getInstance(ComposerCommandRunner::class.java)
    private val running = AtomicBoolean(false)

    fun run(
        command: String,
        workingDirectory: Path?,
        onStarted: () -> Unit = {},
        onCompleted: (success: Boolean) -> Unit = {},
        onFinished: () -> Unit,
    ) {
        if (!running.compareAndSet(false, true)) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Composer Sync Check")
                .createNotification(
                    ComposerSyncCheckBundle.message("command.already.running"),
                    NotificationType.WARNING,
                )
                .notify(project)
            return
        }

        val workDir = workingDirectory?.toFile() ?: project.basePath?.let(::File)
        if (workDir == null) {
            running.set(false)
            logger.warn("Cannot run composer command: missing working directory")
            return
        }

        console.show()
        console.appendLine("[composer-sync-check] Running: ${command.trim()}")
        console.appendLine("[composer-sync-check] Working directory: ${workDir.absolutePath}")
        onStarted()

        AppExecutorUtil.getAppExecutorService().execute {
            try {
                val process = ProcessBuilder(shellArgs(command))
                    .directory(workDir)
                    .start()

                val stdoutThread = thread(start = true, isDaemon = true, name = "composer-sync-stdout") {
                    streamToConsole(process.inputStream, ProcessOutputType.STDOUT)
                }
                val stderrThread = thread(start = true, isDaemon = true, name = "composer-sync-stderr") {
                    streamToConsole(process.errorStream, ProcessOutputType.STDERR)
                }

                val exitCode = process.waitFor()
                stdoutThread.join()
                stderrThread.join()
                console.appendLine("\n[composer-sync-check] Exit code: $exitCode")
                logger.info("Composer command finished with exit code $exitCode")
                if (exitCode == 0) {
                    showCommandResultBalloon(
                        success = true,
                        title = ComposerSyncCheckBundle.message("command.success.title"),
                        message = ComposerSyncCheckBundle.message("command.success.message"),
                    )
                    onCompleted(true)
                } else {
                    showCommandResultBalloon(
                        success = false,
                        title = ComposerSyncCheckBundle.message("command.failure.title"),
                        message = ComposerSyncCheckBundle.message("command.failure.message", exitCode.toString()),
                    )
                    onCompleted(false)
                }
            } catch (t: Throwable) {
                logger.warn("Failed to run composer command", t)
                console.appendLine("[composer-sync-check] Failed: ${t.message}")
                showCommandResultBalloon(
                    success = false,
                    title = ComposerSyncCheckBundle.message("command.failure.title"),
                    message = ComposerSyncCheckBundle.message("command.failure.unexpected", t.message ?: "unknown error"),
                )
                onCompleted(false)
            } finally {
                running.set(false)
                onFinished()
            }
        }
    }

    private fun shellArgs(command: String): List<String> {
        return if (SystemInfo.isWindows) {
            listOf("cmd", "/c", command)
        } else {
            listOf("/bin/bash", "-lc", command)
        }
    }

    private fun streamToConsole(stream: InputStream, outputType: ProcessOutputType) {
        val buffer = ByteArray(4096)
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) return
            val text = String(buffer, 0, read)
            console.appendProcessText(text, outputType)
        }
    }

    private fun showCommandResultBalloon(success: Boolean, title: String, message: String) {
        ApplicationManager.getApplication().invokeLater {
            val frameComponent = WindowManager.getInstance().getIdeFrame(project)?.component
            if (frameComponent == null) {
                NotificationGroupManager.getInstance()
                    .getNotificationGroup("Composer Sync Check")
                    .createNotification(
                        title,
                        message,
                        if (success) NotificationType.INFORMATION else NotificationType.ERROR,
                    )
                    .notify(project)
                return@invokeLater
            }

            val fillColor = if (success) Color(225, 245, 231) else Color(253, 232, 232)
            val textColor = if (success) "#1b5e20" else "#7f1d1d"
            val html = "<b>${escapeHtml(title)}</b><br/>" +
                "<span style=\"color:$textColor;\">${escapeHtml(message)}</span>"

            JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(html, MessageType.INFO, null)
                .setFillColor(fillColor)
                .setFadeoutTime(6000)
                .setHideOnClickOutside(true)
                .setHideOnKeyOutside(true)
                .createBalloon()
                .show(RelativePoint.getCenterOf(frameComponent), Balloon.Position.above)
        }
    }

    private fun escapeHtml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }
}
