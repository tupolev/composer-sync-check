package net.kodesoft.composersynccheck.detection

import java.nio.file.Files
import java.nio.file.Path

data class ComposerSyncCheckResult(
    val isOutOfSync: Boolean,
    val reason: String,
    val fingerprint: String,
)

object ComposerSyncDetector {

    fun evaluate(files: ResolvedComposerFiles, fallbackRoot: Path): ComposerSyncCheckResult {
        val composerJson = files.composerJson
        val composerLock = files.composerLock

        if (composerJson == null && composerLock == null) {
            return ComposerSyncCheckResult(
                isOutOfSync = false,
                reason = "no-composer-project",
                fingerprint = "none",
            )
        }

        val workingDir = files.workingDirectory ?: fallbackRoot
        val vendorDir = workingDir.resolve("vendor")
        val installedJson = workingDir.resolve("vendor/composer/installed.json")

        val lockTs = timestamp(composerLock)
        val jsonTs = timestamp(composerJson)
        val installedTs = timestamp(installedJson)
        val vendorTs = timestamp(vendorDir)

        if (!Files.exists(vendorDir)) {
            return result(true, "vendor-missing", lockTs, jsonTs, installedTs, vendorTs, composerJson, composerLock)
        }

        val referenceTs = maxOf(lockTs, jsonTs)
        if (referenceTs <= 0L) {
            return result(false, "no-reference-files", lockTs, jsonTs, installedTs, vendorTs, composerJson, composerLock)
        }

        if (installedTs > 0L) {
            if (installedTs < referenceTs) {
                return result(true, "installed-json-outdated", lockTs, jsonTs, installedTs, vendorTs, composerJson, composerLock)
            }
            return result(false, "installed-json-current", lockTs, jsonTs, installedTs, vendorTs, composerJson, composerLock)
        }

        if (vendorTs < referenceTs) {
            return result(true, "vendor-outdated", lockTs, jsonTs, installedTs, vendorTs, composerJson, composerLock)
        }

        return result(false, "vendor-current", lockTs, jsonTs, installedTs, vendorTs, composerJson, composerLock)
    }

    private fun result(
        outOfSync: Boolean,
        reason: String,
        lockTs: Long,
        jsonTs: Long,
        installedTs: Long,
        vendorTs: Long,
        jsonPath: Path?,
        lockPath: Path?,
    ): ComposerSyncCheckResult {
        val fingerprint = "lock=$lockTs|json=$jsonTs|installed=$installedTs|vendor=$vendorTs|jsonPath=${jsonPath ?: "-"}|lockPath=${lockPath ?: "-"}|$reason"
        return ComposerSyncCheckResult(outOfSync, reason, fingerprint)
    }

    private fun timestamp(path: Path?): Long {
        if (path == null) return -1L
        return try {
            if (Files.exists(path)) Files.getLastModifiedTime(path).toMillis() else -1L
        } catch (_: Exception) {
            -1L
        }
    }
}
