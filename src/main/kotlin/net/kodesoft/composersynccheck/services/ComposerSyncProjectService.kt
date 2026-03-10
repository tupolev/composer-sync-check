package net.kodesoft.composersynccheck.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import git4idea.GitUtil
import git4idea.changes.GitChangeUtils
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import net.kodesoft.composersynccheck.detection.ComposerFilesLocator
import net.kodesoft.composersynccheck.detection.ComposerSyncDetector
import net.kodesoft.composersynccheck.execution.ComposerCommandRunner
import net.kodesoft.composersynccheck.notifications.ComposerSyncNotificationService
import net.kodesoft.composersynccheck.settings.ComposerSyncCheckSettingsState
import net.kodesoft.composersynccheck.toolwindow.ComposerSyncConsoleService
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Service(Service.Level.PROJECT)
class ComposerSyncProjectService(private val project: Project) : Disposable {

    private val logger = Logger.getInstance(ComposerSyncProjectService::class.java)
    private val settings = project.service<ComposerSyncCheckSettingsState>()
    private val console = project.service<ComposerSyncConsoleService>()
    private val runner = ComposerCommandRunner(project, console)
    private val notifications = ComposerSyncNotificationService(project)
    private val checkAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
    private val periodicAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
    private val gitDiffAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)
    private val gitRepositoryManager = GitRepositoryManager.getInstance(project)

    private val suppressChecksUntil = AtomicLong(0L)
    private val syncCheckRunning = AtomicBoolean(false)
    private val periodicOutOfSyncNotificationsShown = AtomicInteger(0)
    private var ignoredForSession = false
    private var lastNotificationFingerprint: String? = null
    private val previousRevisionByRepoRoot = ConcurrentHashMap<String, String?>()
    private val pendingGitRepoRoots = ConcurrentHashMap.newKeySet<String>()
    private val logTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
    private var lastSyncStateIsInSync = false
    private var lastComposerRunFailed = false
    private var composerCommandRunning = false

    init {
        initializeGitRevisionCache()
        scheduleDebouncedCheck("startup", 1000)
        reschedulePeriodicChecks()
    }

    override fun dispose() {
        checkAlarm.cancelAllRequests()
        periodicAlarm.cancelAllRequests()
        gitDiffAlarm.cancelAllRequests()
    }

    fun onSettingsUpdated() {
        reschedulePeriodicChecks()
        logDebug("Settings updated")
        scheduleDebouncedCheck("settings-updated", 500)
    }

    fun onComposerFilesChanged() {
        if (!settings.state.checkOnComposerFilesChange) return
        logDebug("composer.json/composer.lock file change detected")
        scheduleDebouncedCheck("composer-files-changed", 4000)
    }

    fun onGitRepositoryChanged(repository: GitRepository) {
        if (!settings.state.checkOnGitBranchChange) return
        logDebug("Git repository changed: ${repository.presentableUrl}")
        pendingGitRepoRoots.add(repository.root.path)
        gitDiffAlarm.cancelAllRequests()
        gitDiffAlarm.addRequest({ processPendingGitRepositoryChanges() }, 1500)
    }

    fun runComposerInstallFromToolWindow() {
        if (project.isDisposed) return
        val basePath = project.basePath ?: return
        val projectRoot = Path.of(basePath)
        val resolvedFiles = ComposerFilesLocator.resolve(projectRoot, settings.state)

        val workingDirectory = resolvedFiles.workingDirectory ?: projectRoot
        logToConsole("Manual run requested in tool window")
        runner.run(
            command = settings.state.composerCommand,
            workingDirectory = workingDirectory,
            onStarted = {
                composerCommandRunning = true
                refreshActionButtons()
                refreshStatusIndicator()
            },
            onCompleted = { success ->
                composerCommandRunning = false
                lastComposerRunFailed = !success
                refreshActionButtons()
                refreshStatusIndicator()
            },
            onFinished = {
                suppressChecksUntil.set(System.currentTimeMillis() + 2_000)
                scheduleDebouncedCheck("manual-composer-command-finished", 3000)
            }
        )
    }

    fun runSyncStatusCheckFromToolWindow() {
        if (project.isDisposed) return
        logToConsole("Manual sync status check requested in tool window")
        runCheck(OutOfSyncNotificationPolicy.SOURCE_MANUAL_STATUS_CHECK)
    }

    fun showOutOfSyncNotificationForTesting() {
        if (project.isDisposed) return
        val basePath = project.basePath ?: return
        val projectRoot = Path.of(basePath)
        val resolvedFiles = ComposerFilesLocator.resolve(projectRoot, settings.state)
        showOutOfSyncNotification(resolvedFiles.workingDirectory ?: projectRoot)
    }

    fun scheduleDebouncedCheck(source: String, delayMs: Int) {
        if (project.isDisposed) return
        logDebug("Scheduling check source=$source delayMs=$delayMs")
        checkAlarm.cancelAllRequests()
        checkAlarm.addRequest({ runCheck(source) }, delayMs)
    }

    private fun runCheck(source: String) {
        if (project.isDisposed) return
        if (!syncCheckRunning.compareAndSet(false, true)) {
            logDebug("Skipping check source=$source because another check is already running")
            return
        }
        refreshActionButtons()
        refreshStatusIndicator()

        if (System.currentTimeMillis() < suppressChecksUntil.get()) {
            logDebug("Skipping check source=$source because checks are suppressed")
            syncCheckRunning.set(false)
            refreshActionButtons()
            refreshStatusIndicator()
            return
        }

        val basePath = project.basePath
        if (basePath == null) {
            syncCheckRunning.set(false)
            refreshActionButtons()
            refreshStatusIndicator()
            return
        }

        AppExecutorUtil.getAppExecutorService().execute {
            try {
                logDebug("Running check source=$source")
                val resolvedFiles = ComposerFilesLocator.resolve(Path.of(basePath), settings.state)
                if (!resolvedFiles.hasComposerProject) {
                    lastNotificationFingerprint = null
                    lastSyncStateIsInSync = false
                    refreshStatusIndicator()
                    logDebug("No composer project files resolved for source=$source")
                    return@execute
                }

                val result = try {
                    ComposerSyncDetector.evaluate(resolvedFiles, Path.of(basePath))
                } catch (t: Throwable) {
                    logger.warn("Composer sync check failed for source=$source", t)
                    lastSyncStateIsInSync = false
                    refreshStatusIndicator()
                    logDebug("Check failed source=$source: ${t.message}")
                    return@execute
                }

                logger.debug("Composer check source=$source, result=${result.reason}, outOfSync=${result.isOutOfSync}")
                logDebug("Check result source=$source outOfSync=${result.isOutOfSync} reason=${result.reason}")

                if (!result.isOutOfSync) {
                    lastNotificationFingerprint = null
                    lastSyncStateIsInSync = true
                    refreshStatusIndicator()
                    return@execute
                }

                lastSyncStateIsInSync = false
                refreshStatusIndicator()

                val decision = OutOfSyncNotificationPolicy.decide(
                    source = source,
                    showNotificationBalloon = settings.state.showNotificationBalloon,
                    ignoredForSession = ignoredForSession,
                    lastNotificationFingerprint = lastNotificationFingerprint,
                    currentFingerprint = result.fingerprint,
                    periodicNotificationsShown = periodicOutOfSyncNotificationsShown.get(),
                )
                periodicOutOfSyncNotificationsShown.set(decision.periodicNotificationsShown)
                if (!decision.shouldNotify) {
                    if (source == OutOfSyncNotificationPolicy.SOURCE_PERIODIC &&
                        periodicOutOfSyncNotificationsShown.get() > 2
                    ) {
                        logDebug("Skipping periodic notification source=$source because session limit was reached")
                    }
                    return@execute
                }

                lastNotificationFingerprint = result.fingerprint
                showOutOfSyncNotification(resolvedFiles.workingDirectory ?: Path.of(basePath))
            } finally {
                syncCheckRunning.set(false)
                refreshActionButtons()
                refreshStatusIndicator()
            }
        }
    }

    private fun showOutOfSyncNotification(workingDirectory: Path) {
        notifications.notifyOutOfSync(
            onRunInstall = {
                suppressChecksUntil.set(System.currentTimeMillis() + 15_000)
                logToConsole("Notification action: running composer install")
                runner.run(
                    command = settings.state.composerCommand,
                    workingDirectory = workingDirectory,
                    onStarted = {
                        composerCommandRunning = true
                        refreshActionButtons()
                        refreshStatusIndicator()
                    },
                    onCompleted = { success ->
                        composerCommandRunning = false
                        lastComposerRunFailed = !success
                        refreshActionButtons()
                        refreshStatusIndicator()
                    },
                    onFinished = {
                        suppressChecksUntil.set(System.currentTimeMillis() + 2_000)
                        scheduleDebouncedCheck("composer-command-finished", 3000)
                    }
                )
            },
            onIgnoreSession = {
                ignoredForSession = true
            },
        )
    }

    private fun initializeGitRevisionCache() {
        for (repository in gitRepositoryManager.repositories) {
            previousRevisionByRepoRoot[repository.root.path] = repository.currentRevision
        }
    }

    private fun processPendingGitRepositoryChanges() {
        if (project.isDisposed) return
        val rootsToProcess = pendingGitRepoRoots.toList()
        if (rootsToProcess.isEmpty()) return
        pendingGitRepoRoots.clear()

        val repositoriesByRoot = gitRepositoryManager.repositories.associateBy { it.root.path }
        for (repoRoot in rootsToProcess) {
            val repository = repositoriesByRoot[repoRoot] ?: continue
            checkComposerLockDiffForRepository(repository)
        }
    }

    private fun checkComposerLockDiffForRepository(repository: GitRepository) {
        val repositoryRoot = repository.root.path
        val previousRevision = previousRevisionByRepoRoot[repositoryRoot]
        val currentRevision = repository.currentRevision

        if (currentRevision.isNullOrBlank()) {
            previousRevisionByRepoRoot[repositoryRoot] = currentRevision
            return
        }

        if (previousRevision == null) {
            previousRevisionByRepoRoot[repositoryRoot] = currentRevision
            return
        }

        if (previousRevision == currentRevision) return

        previousRevisionByRepoRoot[repositoryRoot] = currentRevision

        val composerLockChanged = try {
            val diff = GitChangeUtils.getDiff(project, repository.root, previousRevision, currentRevision, null)
            diffContainsComposerLock(repository, diff)
        } catch (e: VcsException) {
            logger.warn(
                "Failed to compute Git diff for ${repository.presentableUrl} from $previousRevision to $currentRevision",
                e,
            )
            false
        } catch (t: Throwable) {
            logger.warn(
                "Unexpected failure while checking composer.lock diff for ${repository.presentableUrl}",
                t,
            )
            false
        }

        logger.debug(
            "Git diff check repo=${repository.presentableUrl}, from=$previousRevision, to=$currentRevision, composerLockChanged=$composerLockChanged",
        )
        logDebug(
            "Git diff checked repo=${repository.presentableUrl} from=$previousRevision to=$currentRevision composerLockChanged=$composerLockChanged",
        )

        if (composerLockChanged) {
            scheduleDebouncedCheck("git-composer-lock-changed", 300)
        }
    }

    private fun diffContainsComposerLock(repository: GitRepository, changes: Collection<Change>): Boolean {
        return changes.any { change ->
            isComposerLockPath(repository, change.beforeRevision?.file) ||
                isComposerLockPath(repository, change.afterRevision?.file)
        }
    }

    private fun isComposerLockPath(repository: GitRepository, filePath: FilePath?): Boolean {
        if (filePath == null) return false
        val relativePath = GitUtil.getRelativePath(repository.root.path, filePath)
        return relativePath == "composer.lock"
    }

    private fun reschedulePeriodicChecks() {
        periodicAlarm.cancelAllRequests()
        val interval = settings.state.checkIntervalMinutes
        if (interval <= 0) {
            logDebug("Periodic checks disabled")
            return
        }
        val intervalMs = interval * 60 * 1000
        logDebug("Scheduling periodic checks intervalMinutes=$interval")
        periodicAlarm.addRequest(
            {
                runCheck("periodic")
                reschedulePeriodicChecks()
            },
            intervalMs,
        )
    }

    private fun logDebug(message: String) {
        if (!settings.state.debugMode) return
        logToConsole("[debug] $message")
    }

    private fun logToConsole(message: String) {
        console.appendLine("[${LocalDateTime.now().format(logTimeFormat)}] $message")
    }

    private fun refreshStatusIndicator() {
        val status = when {
            composerCommandRunning || syncCheckRunning.get() -> ComposerSyncConsoleService.SyncStatus.RUNNING
            lastComposerRunFailed -> ComposerSyncConsoleService.SyncStatus.OUT_OF_SYNC
            lastSyncStateIsInSync -> ComposerSyncConsoleService.SyncStatus.IN_SYNC
            else -> ComposerSyncConsoleService.SyncStatus.OUT_OF_SYNC
        }
        console.setSyncStatus(status)
    }

    fun syncActionButtonsStateToUi() {
        refreshActionButtons()
    }

    private fun refreshActionButtons() {
        val enabled = !composerCommandRunning && !syncCheckRunning.get()
        console.setRunInstallEnabled(enabled)
        console.setCheckStatusEnabled(enabled)
    }
}
