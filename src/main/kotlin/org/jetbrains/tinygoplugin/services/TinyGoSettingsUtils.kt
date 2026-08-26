package org.jetbrains.tinygoplugin.services

import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdk
import com.intellij.execution.RunManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import org.jetbrains.tinygoplugin.runconfig.TinyGoRunConfiguration

fun isTinyGoActive(project: Project, module: Module? = null): Boolean {
    val config = project.tinyGoConfiguration()
    val cached = config.cachedGoRoot
    if (!config.enabled || cached == GoSdk.NULL || cached.srcDir == null) return false
    val mod = module ?: ModuleManager.getInstance(project).modules.firstOrNull() ?: return true
    val buildSettings = GoModuleSettings.getInstance(mod).buildTargetSettings
    val osMatch = config.goOS.isEmpty() || config.goOS == "default" || buildSettings.os == config.goOS
    val archMatch = config.goArch.isEmpty() || config.goArch == "default" || buildSettings.arch == config.goArch
    return osMatch && archMatch
}

fun propagateGoFlags(project: Project, settings: TinyGoConfiguration) {
    val modules = ModuleManager.getInstance(project).modules
    if (modules.isEmpty()) {
        logger<TinyGoSettingsService>().warn("Could not find go modules")
        return
    }
    for (module in modules) {
        val goSettings = GoModuleSettings.getInstance(module)
        val buildSettings = com.goide.project.GoBuildTargetSettings()
        buildSettings.arch = settings.goArch
        buildSettings.os = settings.goOS
        buildSettings.customFlags = settings.goTags.split(' ').filter { it.isNotBlank() }.toTypedArray()
        goSettings.buildTargetSettings = buildSettings
    }
    project.save()
}

fun resetGoFlags(project: Project) {
    val modules = ModuleManager.getInstance(project).modules
    if (modules.isEmpty()) {
        logger<TinyGoSettingsService>().warn("Could not find go modules")
        return
    }
    for (module in modules) {
        val goSettings = GoModuleSettings.getInstance(module)
        val buildSettings = com.goide.project.GoBuildTargetSettings()
        goSettings.buildTargetSettings = buildSettings
    }
    project.save()
}

fun updateTinyGoRunConfigurations(project: Project, settings: TinyGoConfiguration) {
    val configurations = RunManager.getInstance(project)
        .allConfigurationsList
        .filterIsInstance<TinyGoRunConfiguration>()
    configurations.forEach {
        it.runConfig.targetPlatform = settings.targetPlatform
        it.runConfig.scheduler = settings.scheduler
        it.runConfig.gc = settings.gc
    }
}

fun tinyGoArguments(settings: TinyGoConfiguration): List<String> {
    val parameters = mutableListOf("-target", settings.targetPlatform)
    if (settings.scheduler != Scheduler.AUTO_DETECT) {
        parameters.addAll(listOf("-scheduler", settings.scheduler.cmd))
    }
    if (settings.gc != GarbageCollector.AUTO_DETECT) {
        parameters.addAll(listOf("-gc", settings.gc.cmd))
    }
    return parameters
}
