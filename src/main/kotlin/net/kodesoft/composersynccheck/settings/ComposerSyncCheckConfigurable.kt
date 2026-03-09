package net.kodesoft.composersynccheck.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.UIUtil
import net.kodesoft.composersynccheck.ComposerSyncCheckBundle
import net.kodesoft.composersynccheck.detection.ComposerFilesLocator
import net.kodesoft.composersynccheck.services.ComposerSyncProjectService
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel

class ComposerSyncCheckConfigurable(private val project: Project) : SearchableConfigurable, Configurable.NoScroll {

    private val settings = project.service<ComposerSyncCheckSettingsState>()

    private val composerCommandField = JBTextField()
    private val composerJsonPathField = JBTextField()
    private val composerLockPathField = JBTextField()
    private val checkIntervalSpinner = JSpinner(SpinnerNumberModel(10, 0, 1440, 1))
    private val gitCheckBox = JBCheckBox()
    private val fileCheckBox = JBCheckBox()
    private val notificationCheckBox = JBCheckBox()
    private val debugModeCheckBox = JBCheckBox()
    private var validatorsInstalled = false

    override fun getId(): String = "tools.composer-sync-check"

    override fun getDisplayName(): String = ComposerSyncCheckBundle.message("settings.display.name")

    override fun getPreferredFocusedComponent(): JComponent = composerCommandField

    override fun createComponent(): JComponent {
        val panel = JPanel(GridBagLayout()).apply {
            val gc = GridBagConstraints().apply {
                anchor = GridBagConstraints.WEST
                fill = GridBagConstraints.HORIZONTAL
                weightx = 1.0
                gridx = 0
                gridy = 0
                insets = com.intellij.util.ui.JBUI.insets(4)
            }

            add(JLabel(ComposerSyncCheckBundle.message("settings.composer.command")), gc)
            gc.gridy++
            add(composerCommandField, gc)

            gc.gridy++
            add(JLabel(ComposerSyncCheckBundle.message("settings.composer.json.path")), gc)
            gc.gridy++
            add(
                createPathFieldWithAutoDetectButton(composerJsonPathField) {
                    autoDetectIntoField("composer.json", composerJsonPathField)
                },
                gc
            )

            gc.gridy++
            add(JLabel(ComposerSyncCheckBundle.message("settings.composer.lock.path")), gc)
            gc.gridy++
            add(
                createPathFieldWithAutoDetectButton(composerLockPathField) {
                    autoDetectIntoField("composer.lock", composerLockPathField)
                },
                gc
            )

            gc.gridy++
            add(JLabel(ComposerSyncCheckBundle.message("settings.check.interval")), gc)
            gc.gridy++
            add(checkIntervalSpinner, gc)

            gc.gridy++
            add(TitledSeparator(ComposerSyncCheckBundle.message("settings.section.checks")), gc)

            gc.gridy++
            gitCheckBox.text = ComposerSyncCheckBundle.message("settings.check.git")
            add(gitCheckBox, gc)
            gc.gridy++
            addDescriptionLabel(this, ComposerSyncCheckBundle.message("settings.desc.check.git"), gc)

            gc.gridy++
            fileCheckBox.text = ComposerSyncCheckBundle.message("settings.check.files")
            add(fileCheckBox, gc)
            gc.gridy++
            addDescriptionLabel(this, ComposerSyncCheckBundle.message("settings.desc.check.files"), gc)

            gc.gridy++
            notificationCheckBox.text = ComposerSyncCheckBundle.message("settings.notifications")
            add(notificationCheckBox, gc)
            gc.gridy++
            addDescriptionLabel(this, ComposerSyncCheckBundle.message("settings.desc.notifications"), gc)

            gc.gridy++
            debugModeCheckBox.text = ComposerSyncCheckBundle.message("settings.debug.mode")
            add(debugModeCheckBox, gc)
            gc.gridy++
            addDescriptionLabel(this, ComposerSyncCheckBundle.message("settings.desc.debug.mode"), gc)

            gc.gridy++
            gc.weighty = 1.0
            add(JPanel(), gc)
        }
        installValidators()
        return panel
    }

    override fun isModified(): Boolean {
        val state = settings.state
        return composerCommandField.text.trim() != state.composerCommand ||
            composerJsonPathField.text.trim() != state.composerJsonPath ||
            composerLockPathField.text.trim() != state.composerLockPath ||
            (checkIntervalSpinner.value as Int) != state.checkIntervalMinutes ||
            gitCheckBox.isSelected != state.checkOnGitBranchChange ||
            fileCheckBox.isSelected != state.checkOnComposerFilesChange ||
            notificationCheckBox.isSelected != state.showNotificationBalloon ||
            debugModeCheckBox.isSelected != state.debugMode
    }

    override fun apply() {
        validate()

        settings.update {
            it.composerCommand = composerCommandField.text.trim()
            it.composerJsonPath = composerJsonPathField.text.trim()
            it.composerLockPath = composerLockPathField.text.trim()
            it.checkIntervalMinutes = checkIntervalSpinner.value as Int
            it.checkOnGitBranchChange = gitCheckBox.isSelected
            it.checkOnComposerFilesChange = fileCheckBox.isSelected
            it.showNotificationBalloon = notificationCheckBox.isSelected
            it.debugMode = debugModeCheckBox.isSelected
        }
        project.service<ComposerSyncProjectService>().onSettingsUpdated()
    }

    override fun reset() {
        val state = settings.state
        composerCommandField.text = state.composerCommand
        composerJsonPathField.text = state.composerJsonPath
        composerLockPathField.text = state.composerLockPath
        checkIntervalSpinner.value = state.checkIntervalMinutes
        gitCheckBox.isSelected = state.checkOnGitBranchChange
        fileCheckBox.isSelected = state.checkOnComposerFilesChange
        notificationCheckBox.isSelected = state.showNotificationBalloon
        debugModeCheckBox.isSelected = state.debugMode
    }

    private fun createPathFieldWithAutoDetectButton(field: JBTextField, onAutoDetect: () -> Unit): JComponent {
        return JPanel(BorderLayout(com.intellij.util.ui.JBUI.scale(8), 0)).apply {
            add(field, BorderLayout.CENTER)
            add(JButton(ComposerSyncCheckBundle.message("settings.autodetect")).apply { addActionListener { onAutoDetect() } }, BorderLayout.EAST)
        }
    }

    private fun addDescriptionLabel(container: JPanel, text: String, gc: GridBagConstraints) {
        container.add(
            JLabel(text).apply {
                foreground = UIUtil.getContextHelpForeground()
                font = font.deriveFont(font.size2D - 1f)
            },
            gc
        )
    }

    private fun autoDetectIntoField(fileName: String, field: JBTextField) {
        val projectRoot = project.basePath?.let(Path::of) ?: run {
            Messages.showErrorDialog(
                project,
                ComposerSyncCheckBundle.message("settings.error.project.root.missing"),
                ComposerSyncCheckBundle.message("settings.display.name")
            )
            return
        }

        val detectedPath = ComposerFilesLocator.autoDetect(projectRoot, fileName)
        if (detectedPath == null) {
            Messages.showErrorDialog(
                project,
                ComposerSyncCheckBundle.message("settings.error.autodetect.not.found", fileName),
                ComposerSyncCheckBundle.message("settings.display.name")
            )
            return
        }

        field.text = toConfigPath(projectRoot, detectedPath)
    }

    private fun validate() {
        val command = composerCommandField.text.trim()
        if (command.isEmpty()) {
            throw ConfigurationException(ComposerSyncCheckBundle.message("settings.error.command.empty"))
        }

        val projectRoot = project.basePath?.let(Path::of) ?: throw ConfigurationException(
            ComposerSyncCheckBundle.message("settings.error.project.root.missing")
        )
        validateRequiredFilePath(
            configuredPath = composerJsonPathField.text.trim(),
            expectedFileName = "composer.json",
            projectRoot = projectRoot,
            emptyErrorKey = "settings.error.composer.json.empty",
            wrongFileErrorKey = "settings.error.composer.json.invalid.file"
        )
        validateRequiredFilePath(
            configuredPath = composerLockPathField.text.trim(),
            expectedFileName = "composer.lock",
            projectRoot = projectRoot,
            emptyErrorKey = "settings.error.composer.lock.empty",
            wrongFileErrorKey = "settings.error.composer.lock.invalid.file"
        )
    }

    private fun validateRequiredFilePath(
        configuredPath: String,
        expectedFileName: String,
        projectRoot: Path,
        emptyErrorKey: String,
        wrongFileErrorKey: String,
    ) {
        if (configuredPath.isEmpty()) {
            throw ConfigurationException(ComposerSyncCheckBundle.message(emptyErrorKey))
        }

        val resolvedPath = resolvePath(projectRoot, configuredPath)
        if (!Files.exists(resolvedPath)) {
            throw ConfigurationException(ComposerSyncCheckBundle.message("settings.error.path.not.found", resolvedPath.toString()))
        }
        if (!Files.isRegularFile(resolvedPath)) {
            throw ConfigurationException(ComposerSyncCheckBundle.message("settings.error.path.not.file", resolvedPath.toString()))
        }
        if (resolvedPath.fileName.toString() != expectedFileName) {
            throw ConfigurationException(ComposerSyncCheckBundle.message(wrongFileErrorKey, expectedFileName))
        }
    }

    private fun resolvePath(projectRoot: Path, configuredPath: String): Path {
        val candidate = Path.of(configuredPath)
        return if (candidate.isAbsolute) candidate.normalize() else projectRoot.resolve(candidate).normalize()
    }

    private fun toConfigPath(projectRoot: Path, detectedPath: Path): String {
        return try {
            projectRoot.normalize().relativize(detectedPath.normalize()).toString()
        } catch (_: IllegalArgumentException) {
            detectedPath.toString()
        }
    }

    private fun installValidators() {
        if (validatorsInstalled) return
        ComponentValidator(project)
            .withValidator {
                val text = composerJsonPathField.text.trim()
                if (!isPathValid(text, "composer.json")) {
                    ValidationInfo("composer.json file not found", composerJsonPathField)
                } else {
                    null
                }
            }
            .installOn(composerJsonPathField)

        ComponentValidator(project)
            .withValidator {
                val text = composerLockPathField.text.trim()
                if (!isPathValid(text, "composer.lock")) {
                    ValidationInfo("composer.lock file not found", composerLockPathField)
                } else {
                    null
                }
            }
            .installOn(composerLockPathField)
        validatorsInstalled = true
    }

    private fun isPathValid(configuredPath: String, expectedFileName: String): Boolean {
        if (configuredPath.isEmpty()) return false
        val projectRoot = project.basePath?.let(Path::of) ?: return false
        val resolvedPath = resolvePath(projectRoot, configuredPath)
        return Files.exists(resolvedPath) &&
            Files.isRegularFile(resolvedPath) &&
            resolvedPath.fileName.toString() == expectedFileName
    }
}
