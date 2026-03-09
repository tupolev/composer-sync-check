package net.kodesoft.composersynccheck.detection

import net.kodesoft.composersynccheck.settings.ComposerSyncCheckSettingsState
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.io.path.name

data class ResolvedComposerFiles(
    val composerJson: Path?,
    val composerLock: Path?,
) {
    val hasComposerProject: Boolean
        get() = composerJson != null || composerLock != null

    val workingDirectory: Path?
        get() = composerJson?.parent ?: composerLock?.parent
}

object ComposerFilesLocator {
    private const val AUTO_DETECT_DEPTH = 6

    fun resolve(projectRoot: Path, state: ComposerSyncCheckSettingsState.State): ResolvedComposerFiles {
        val configuredJson = resolveConfiguredPath(projectRoot, state.composerJsonPath)
        val configuredLock = resolveConfiguredPath(projectRoot, state.composerLockPath)

        val autoJson = if (configuredJson == null) autoDetect(projectRoot, "composer.json") else null
        val autoLock = if (configuredLock == null) autoDetect(projectRoot, "composer.lock") else null

        return ResolvedComposerFiles(
            composerJson = configuredJson ?: autoJson,
            composerLock = configuredLock ?: autoLock,
        )
    }

    fun autoDetect(projectRoot: Path, fileName: String): Path? = findAutoDetected(projectRoot, fileName)

    private fun resolveConfiguredPath(projectRoot: Path, configured: String): Path? {
        if (configured.isBlank()) return null
        val candidate = Path.of(configured)
        val resolved = if (candidate.isAbsolute) candidate else projectRoot.resolve(candidate).normalize()
        return resolved.takeIf { Files.exists(it) }
    }

    private fun findAutoDetected(projectRoot: Path, fileName: String): Path? {
        val rootCandidate = projectRoot.resolve(fileName)
        if (Files.exists(rootCandidate)) return rootCandidate

        return Files.walk(projectRoot, AUTO_DETECT_DEPTH).use { stream ->
            stream
                .filter { it.name == fileName }
                .min(Comparator.comparingInt { it.nameCount })
                .orElse(null)
        }
    }
}
