package net.kodesoft.composersynccheck

import net.kodesoft.composersynccheck.detection.ComposerFilesLocator
import net.kodesoft.composersynccheck.settings.ComposerSyncCheckSettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files

class ComposerFilesLocatorTest {

    @Test
    fun `autodetect finds root composer files`() {
        val root = Files.createTempDirectory("composer-sync-check-test")
        val composerJson = root.resolve("composer.json")
        val composerLock = root.resolve("composer.lock")
        Files.writeString(composerJson, "{}")
        Files.writeString(composerLock, "{}")

        val resolved = ComposerFilesLocator.resolve(root, ComposerSyncCheckSettingsState.State())

        assertEquals(composerJson, resolved.composerJson)
        assertEquals(composerLock, resolved.composerLock)
    }

    @Test
    fun `configured relative paths are resolved from project root`() {
        val root = Files.createTempDirectory("composer-sync-check-test")
        val composerJson = root.resolve("apps/api/composer.json")
        Files.createDirectories(composerJson.parent)
        Files.writeString(composerJson, "{}")

        val state = ComposerSyncCheckSettingsState.State(
            composerJsonPath = "apps/api/composer.json",
            composerLockPath = "",
        )

        val resolved = ComposerFilesLocator.resolve(root, state)

        assertEquals(composerJson, resolved.composerJson)
    }

    @Test
    fun `missing configured path falls back to autodetect`() {
        val root = Files.createTempDirectory("composer-sync-check-test")
        val composerJson = root.resolve("packages/app/composer.json")
        Files.createDirectories(composerJson.parent)
        Files.writeString(composerJson, "{}")

        val state = ComposerSyncCheckSettingsState.State(composerJsonPath = "missing/composer.json")
        val resolved = ComposerFilesLocator.resolve(root, state)

        assertEquals(composerJson, resolved.composerJson)
    }

    @Test
    fun `returns null when no files are found`() {
        val root = Files.createTempDirectory("composer-sync-check-test")

        val resolved = ComposerFilesLocator.resolve(root, ComposerSyncCheckSettingsState.State())

        assertNull(resolved.composerJson)
        assertNull(resolved.composerLock)
    }
}
