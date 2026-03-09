package net.kodesoft.composersynccheck

import net.kodesoft.composersynccheck.detection.ComposerSyncDetector
import net.kodesoft.composersynccheck.detection.ResolvedComposerFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.attribute.FileTime

class ComposerSyncDetectorTest {

    @Test
    fun `returns in sync when no composer files exist`() {
        val root = Files.createTempDirectory("composer-sync-check-test")

        val result = ComposerSyncDetector.evaluate(ResolvedComposerFiles(null, null), root)

        assertFalse(result.isOutOfSync)
    }

    @Test
    fun `returns out of sync when vendor is missing`() {
        val root = Files.createTempDirectory("composer-sync-check-test")
        Files.writeString(root.resolve("composer.json"), "{}")

        val result = ComposerSyncDetector.evaluate(
            ResolvedComposerFiles(root.resolve("composer.json"), null),
            root,
        )

        assertTrue(result.isOutOfSync)
    }

    @Test
    fun `returns out of sync when lock is newer than installed json`() {
        val root = Files.createTempDirectory("composer-sync-check-test")
        val lock = root.resolve("composer.lock")
        Files.writeString(lock, "{}")

        val installed = root.resolve("vendor/composer/installed.json")
        Files.createDirectories(installed.parent)
        Files.writeString(installed, "{}")

        val now = System.currentTimeMillis()
        Files.setLastModifiedTime(installed, FileTime.fromMillis(now))
        Files.setLastModifiedTime(lock, FileTime.fromMillis(now + 5_000))

        val result = ComposerSyncDetector.evaluate(
            ResolvedComposerFiles(null, lock),
            root,
        )

        assertTrue(result.isOutOfSync)
    }

    @Test
    fun `returns in sync when installed json is newer than lock`() {
        val root = Files.createTempDirectory("composer-sync-check-test")
        val lock = root.resolve("composer.lock")
        Files.writeString(lock, "{}")

        val installed = root.resolve("vendor/composer/installed.json")
        Files.createDirectories(installed.parent)
        Files.writeString(installed, "{}")

        val now = System.currentTimeMillis()
        Files.setLastModifiedTime(lock, FileTime.fromMillis(now))
        Files.setLastModifiedTime(installed, FileTime.fromMillis(now + 5_000))

        val result = ComposerSyncDetector.evaluate(
            ResolvedComposerFiles(null, lock),
            root,
        )

        assertFalse(result.isOutOfSync)
    }
}
