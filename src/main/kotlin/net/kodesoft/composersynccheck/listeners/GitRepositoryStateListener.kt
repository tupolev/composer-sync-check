package net.kodesoft.composersynccheck.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import net.kodesoft.composersynccheck.services.ComposerSyncProjectService
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener

class GitRepositoryStateListener(private val project: Project) : GitRepositoryChangeListener {

    override fun repositoryChanged(repository: GitRepository) {
        if (project.isDisposed) return
        project.service<ComposerSyncProjectService>().onGitRepositoryChanged(repository)
    }
}
