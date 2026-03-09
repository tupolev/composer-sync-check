package net.kodesoft.composersynccheck.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.components.service
import net.kodesoft.composersynccheck.services.ComposerSyncProjectService

class ComposerFileChangeListener(private val project: Project) : BulkFileListener {

    override fun after(events: List<VFileEvent>) {
        if (project.isDisposed) return

        val relevant = events.any { event ->
            val path = event.path
            path.endsWith("/composer.json") || path.endsWith("/composer.lock")
        }

        if (relevant) {
            project.service<ComposerSyncProjectService>().onComposerFilesChanged()
        }
    }
}
