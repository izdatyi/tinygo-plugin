package org.jetbrains.tinygoplugin.configuration

import com.goide.GoLibrariesUtil
import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.goide.util.GoUtil
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.ui.EditorNotifications
import com.intellij.util.application
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.messages.MessageBus
import java.util.EventListener

internal class CachedGoRootUpdater : GoModuleSettings.BuildTargetListener {
    companion object {
        val logger = logger<CachedGoRootUpdater>()
    }

    override fun changed(module: Module, batchUpdate: Boolean) {
        val project = module.project
        if (!project.isDisposed) {
            application.invokeLater {
                if (!project.isDisposed) {
                    updateExtLibrariesAndCleanCache(project)
                }
            }
        }
    }
}

interface TinyGoExtractionFailureListener : EventListener {
    fun onExtractionFailure()
}

class CachedGoRootInvalidator(private val project: Project) : TinyGoExtractionFailureListener {
    override fun onExtractionFailure() {
        val tinyGoSettings = project.tinyGoConfiguration()
        tinyGoSettings.cachedGoRoot = GoSdk.NULL
        tinyGoSettings.saveState(project)
        updateExtLibrariesAndCleanCache(project)
    }
}

@RequiresEdt
internal fun updateExtLibrariesAndCleanCache(project: Project) {
    if (!project.isDisposed) {
        application.assertIsDispatchThread()
        project.service<GoSdkService>().incModificationCount()
        GoUtil.cleanResolveCache(project)
        GoLibrariesUtil.updateLibraries(project, RootsChangeRescanningInfo.TOTAL_RESCAN, { }, null)
        DaemonCodeAnalyzer.getInstance(project).restart()
        EditorNotifications.getInstance(project).updateAllNotifications()
    }
}

fun sendReloadLibrariesSignal(project: Project) {
    if (!project.isDisposed) {
        val messageBus: MessageBus = project.messageBus
        val modules = ModuleManager.getInstance(project).modules
        modules.forEach { messageBus.syncPublisher(GoModuleSettings.BUILD_TARGET_TOPIC).changed(it, true) }
    }
}
