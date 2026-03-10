package net.kodesoft.composersynccheck.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import net.kodesoft.composersynccheck.ComposerSyncCheckBundle
import net.kodesoft.composersynccheck.settings.ComposerSyncCheckConfigurable
import net.kodesoft.composersynccheck.services.ComposerSyncProjectService
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.awt.FlowLayout
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.swing.JScrollPane
import javax.swing.JButton
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JTextPane
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

@Service(Service.Level.PROJECT)
class ComposerSyncConsoleService(private val project: Project) : Disposable {

    enum class SyncStatus {
        IN_SYNC,
        OUT_OF_SYNC,
        RUNNING,
    }

    private class NoWrapTextPane : JTextPane() {
        override fun getScrollableTracksViewportWidth(): Boolean = false
    }

    private val textPane = NoWrapTextPane()
    private val ansiDecoder = AnsiEscapeDecoder()
    private val statusIconLabel = JBLabel("\u25CF")
    private lateinit var checkStatusButton: JButton
    private lateinit var runInstallButton: JButton

    init {
        textPane.isEditable = false
        textPane.margin = JBUI.insets(8)
        textPane.background = Color.BLACK
        textPane.foreground = Color.WHITE
        textPane.caretColor = Color.WHITE
        textPane.font = JBUI.Fonts.create("JetBrains Mono", JBUI.scale(12))
        textPane.componentPopupMenu = createConsolePopupMenu()
        statusIconLabel.font = JBUI.Fonts.create("JetBrains Mono", JBUI.scale(14))
        setSyncStatus(SyncStatus.OUT_OF_SYNC)
    }

    fun createContent(): JPanel = JPanel(BorderLayout()).apply {
        add(
            JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(8), JBUI.scale(8))).apply {
                add(JBLabel(ComposerSyncCheckBundle.message("run.status.label")))
                add(statusIconLabel)
                add(
                    JButton(ComposerSyncCheckBundle.message("run.button.check.status"), AllIcons.Actions.Find).apply {
                        checkStatusButton = this
                        addActionListener {
                            project.service<ComposerSyncProjectService>().runSyncStatusCheckFromToolWindow()
                        }
                    },
                )
                add(
                    JButton(ComposerSyncCheckBundle.message("run.button.manual.install"), AllIcons.Actions.Execute).apply {
                        runInstallButton = this
                        addActionListener {
                            project.service<ComposerSyncProjectService>().runComposerInstallFromToolWindow()
                        }
                    },
                )
                add(
                    JButton(AllIcons.General.GearPlain).apply {
                        toolTipText = ComposerSyncCheckBundle.message("run.button.open.settings.tooltip")
                        addActionListener {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project, ComposerSyncCheckConfigurable::class.java)
                        }
                    },
                )
            },
            BorderLayout.NORTH,
        )
        add(
            JBScrollPane(textPane).apply {
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            },
            BorderLayout.CENTER
        )
        project.service<ComposerSyncProjectService>().syncActionButtonsStateToUi()
    }

    fun show() {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            ToolWindowManager.getInstance(project).getToolWindow("Composer Sync Check")?.show()
        }
    }

    fun clear() {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            textPane.text = ""
        }
    }

    fun appendLine(line: String) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            appendStyledText("$line\n", ConsoleViewContentType.SYSTEM_OUTPUT)
        }
    }

    fun appendProcessText(text: String, outputType: Key<*>) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            ansiDecoder.escapeText(
                text,
                outputType,
                object : AnsiEscapeDecoder.ColoredTextAcceptor {
                    override fun coloredTextAvailable(text: String, attributes: Key<*>) {
                        appendStyledText(text, ConsoleViewContentType.getConsoleViewType(attributes))
                    }
                },
            )
        }
    }

    fun setSyncStatus(status: SyncStatus) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            statusIconLabel.foreground = when (status) {
                SyncStatus.IN_SYNC -> Color(46, 160, 67)
                SyncStatus.OUT_OF_SYNC -> Color(220, 53, 69)
                SyncStatus.RUNNING -> Color(255, 193, 7)
            }
        }
    }

    fun setRunInstallEnabled(enabled: Boolean) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            if (::runInstallButton.isInitialized) {
                runInstallButton.isEnabled = enabled
            }
        }
    }

    fun setCheckStatusEnabled(enabled: Boolean) {
        com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
            if (::checkStatusButton.isInitialized) {
                checkStatusButton.isEnabled = enabled
            }
        }
    }

    override fun dispose() {
    }

    private fun createConsolePopupMenu(): JPopupMenu {
        return JPopupMenu().apply {
            add(JMenuItem("Cut").apply { addActionListener { textPane.cut() } })
            add(JMenuItem("Copy").apply { addActionListener { textPane.copy() } })
            add(JMenuItem("Paste").apply { addActionListener { textPane.paste() } })
            addSeparator()
            add(JMenuItem("Select All").apply { addActionListener { textPane.selectAll() } })
            add(
                JMenuItem("Search selection in Google").apply {
                    addActionListener {
                        val selection = textPane.selectedText?.trim().orEmpty()
                        if (selection.isEmpty()) return@addActionListener
                        val encoded = URLEncoder.encode(selection, StandardCharsets.UTF_8)
                        BrowserUtil.browse("https://www.google.com/search?q=$encoded")
                    }
                },
            )
        }
    }

    private fun appendStyledText(text: String, contentType: ConsoleViewContentType) {
        val document = textPane.document as StyledDocument
        val attributes = SimpleAttributeSet().apply {
            val fg = contentType.attributes.foregroundColor ?: Color.WHITE
            val bg = contentType.attributes.backgroundColor ?: Color.BLACK
            val isBold = (contentType.attributes.fontType and Font.BOLD) != 0
            val isItalic = (contentType.attributes.fontType and Font.ITALIC) != 0

            StyleConstants.setForeground(this, fg)
            StyleConstants.setBackground(this, bg)
            StyleConstants.setBold(this, isBold)
            StyleConstants.setItalic(this, isItalic)
            StyleConstants.setFontFamily(this, textPane.font.family)
            StyleConstants.setFontSize(this, textPane.font.size)
        }
        document.insertString(document.length, text, attributes)
        textPane.caretPosition = document.length
    }
}
