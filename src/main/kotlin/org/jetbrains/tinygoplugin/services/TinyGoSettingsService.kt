package org.jetbrains.tinygoplugin.services

import com.goide.project.GoModuleSettings
import com.goide.sdk.GoSdk
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.tinygoplugin.configuration.CachedGoRootInvalidator
import org.jetbrains.tinygoplugin.configuration.ConfigurationWithHistory
import org.jetbrains.tinygoplugin.configuration.GarbageCollector
import org.jetbrains.tinygoplugin.configuration.Scheduler
import org.jetbrains.tinygoplugin.configuration.TinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.sendReloadLibrariesSignal
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import org.jetbrains.tinygoplugin.configuration.updateExtLibrariesAndCleanCache
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.nullSdk
import org.jetbrains.tinygoplugin.ui.ConfigurationProvider
import org.jetbrains.tinygoplugin.ui.TinyGoPropertiesWrapper
import org.jetbrains.tinygoplugin.ui.generateSettingsPanel

class TinyGoConfigurationWithTagUpdate(
    private val settings: TinyGoConfiguration,
    project: Project,
    private val callback: () -> Unit,
) :
    TinyGoConfiguration by settings {

    constructor(project: Project, callback: () -> Unit) : this(ConfigurationWithHistory(project), project, callback)

    override var sdk: TinyGoSdk
        get() = settings.sdk
        set(value) {
            if (settings.sdk != value) {
                settings.sdk = value
                if (value != nullSdk && settings.targetPlatform.isNotEmpty()) {
                    callback()
                } else if (value == nullSdk) {
                    settings.targetPlatform = ""
                    settings.goOS = ""
                    settings.goArch = ""
                    settings.goTags = ""
                }
            }
        }

    override var targetPlatform: String
        get() = settings.targetPlatform
        set(value) {
            if (settings.targetPlatform != value) {
                settings.targetPlatform = value
                if (value.isNotEmpty()) {
                    settings.gc = GarbageCollector.AUTO_DETECT
                    settings.scheduler = Scheduler.AUTO_DETECT
                    if (settings.sdk != nullSdk) {
                        callback()
                    }
                }
            }
        }

    override var gc: GarbageCollector
        get() = settings.gc
        set(value) {
            if (settings.gc != value) {
                settings.gc = value
                if (settings.sdk != nullSdk && settings.targetPlatform.isNotEmpty()) {
                    callback()
                }
            }
        }

    override var scheduler: Scheduler
        get() = settings.scheduler
        set(value) {
            if (settings.scheduler != value) {
                settings.scheduler = value
                if (settings.sdk != nullSdk && settings.targetPlatform.isNotEmpty()) {
                    callback()
                }
            }
        }

    private fun goSettings(project: Project): GoModuleSettings? =
        ModuleManager.getInstance(project).modules.firstNotNullOfOrNull {
            GoModuleSettings.getInstance(it)
        }

    override fun modified(project: Project): Boolean {
        val moduleSettings = goSettings(project)
        if (moduleSettings != null && settings.enabled) {
            val buildSettings = moduleSettings.buildTargetSettings
            if (settings.goArch != buildSettings.arch ||
                settings.goOS != buildSettings.os ||
                settings.goTags != buildSettings.customFlags.joinToString(" ")
            ) {
                return true
            }
        }
        return settings.modified(project)
    }
}

class TinyGoSettingsService(private val project: Project) :
    BoundConfigurable("TinyGo"), ConfigurationProvider<TinyGoConfiguration> {
    // local copy of the settings
    override var tinyGoSettings: TinyGoConfiguration =
        TinyGoConfigurationWithTagUpdate(project, this::callExtractor)

    private val propertiesWrapper = TinyGoPropertiesWrapper(this)

    override fun isModified(): Boolean = tinyGoSettings.modified(project)

    override fun apply() {
        super.apply()
        thisLogger().warn("Apply called")
        val oldConfiguration = project.tinyGoConfiguration()
        val oldSdk = oldConfiguration.sdk
        val oldTarget = oldConfiguration.targetPlatform
        val oldGoOS = oldConfiguration.goOS
        val oldGoArch = oldConfiguration.goArch
        val oldGoTags = oldConfiguration.goTags

        if (!tinyGoSettings.enabled) {
            tinyGoSettings.targetPlatform = ""
            tinyGoSettings.goOS = ""
            tinyGoSettings.goArch = ""
            tinyGoSettings.goTags = ""
            tinyGoSettings.cachedGoRoot = GoSdk.NULL
            resetGoFlags(project)
        } else {
            propagateGoFlags()
            updateTinyGoRunConfigurations()
        }
        tinyGoSettings.saveState(project)
        updateExtLibrariesAndCleanCache(project)
        if (oldSdk != tinyGoSettings.sdk || oldTarget != tinyGoSettings.targetPlatform ||
            oldGoOS != tinyGoSettings.goOS || oldGoArch != tinyGoSettings.goArch || oldGoTags != tinyGoSettings.goTags
        ) {
            sendReloadLibrariesSignal(project)
        }
    }

    override fun createPanel(): DialogPanel = generateSettingsPanel(project, propertiesWrapper, disposable!!)

    private fun callExtractor() {
        if (tinyGoSettings.sdk == nullSdk || tinyGoSettings.targetPlatform.isEmpty()) {
            return
        }
        TinyGoServiceScope.getScope(project).launch(ModalityState.current().asContextElement()) {
            project.service<TinyGoInfoExtractor>()
                .extractTinyGoInfo(tinyGoSettings, CachedGoRootInvalidator(project)) { _, output ->
                    TinyGoServiceScope.getScope(project).launch(ModalityState.current().asContextElement()) {
                        thisLogger().trace(output)
                        tinyGoSettings.extractTinyGoInfo(output)
                        withContext(Dispatchers.EDT) {
                            // update all ui fields
                            propertiesWrapper.reset()
                        }
                    }
                }
        }
    }

    override fun reset() {
        tinyGoSettings = TinyGoConfigurationWithTagUpdate(project, this::callExtractor)
        propertiesWrapper.reset()
        super.reset()
    }

    private fun propagateGoFlags() {
        propagateGoFlags(project, tinyGoSettings)
    }

    private fun updateTinyGoRunConfigurations() {
        updateTinyGoRunConfigurations(project, tinyGoSettings)
    }
}
