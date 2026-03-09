package net.kodesoft.composersynccheck.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@Service(Service.Level.PROJECT)
@State(name = "ComposerSyncCheckSettings", storages = [Storage("composer-sync-check.xml")])
class ComposerSyncCheckSettingsState : PersistentStateComponent<ComposerSyncCheckSettingsState.State> {

    data class State(
        var composerCommand: String = "composer install",
        var composerJsonPath: String = "",
        var composerLockPath: String = "",
        var checkIntervalMinutes: Int = 10,
        var checkOnGitBranchChange: Boolean = true,
        var checkOnComposerFilesChange: Boolean = true,
        var showNotificationBalloon: Boolean = true,
        var debugMode: Boolean = false,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state.copy(
            checkIntervalMinutes = state.checkIntervalMinutes.coerceAtLeast(0),
            composerCommand = state.composerCommand.ifBlank { "composer install" },
            composerJsonPath = state.composerJsonPath.trim(),
            composerLockPath = state.composerLockPath.trim(),
        )
    }

    fun update(block: (State) -> Unit) {
        block(state)
        state = state.copy(
            checkIntervalMinutes = state.checkIntervalMinutes.coerceAtLeast(0),
            composerCommand = state.composerCommand.ifBlank { "composer install" },
            composerJsonPath = state.composerJsonPath.trim(),
            composerLockPath = state.composerLockPath.trim(),
        )
    }
}
