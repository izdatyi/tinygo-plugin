package org.jetbrains.tinygoplugin.configuration

import com.goide.sdk.GoSdk
import com.goide.util.GoUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.nullSdk
import org.jetbrains.tinygoplugin.sdk.unknownVersion

interface TinyGoConfiguration :
    UserConfiguration,
    ProjectConfiguration {
    fun deepCopy(): TinyGoConfiguration

    fun saveState(project: Project)

    fun modified(project: Project): Boolean

    val enabled: Boolean

    companion object {
        fun getInstance(p: Project): TinyGoConfiguration = TinyGoConfigurationImpl(p).deepCopy()

        fun getInstance(): TinyGoConfiguration = TinyGoConfigurationImpl()
    }
}

internal data class TinyGoConfigurationImpl(
    private val userConfig: UserConfigurationStorageWrapper = UserConfigurationStorageWrapper(),
    private val projectConfig: ProjectConfigurationState = ProjectConfigurationState(),
) : TinyGoConfiguration,
    UserConfiguration by userConfig,
    ProjectConfiguration by projectConfig {
    constructor(project: Project) : this(
        projectConfig = project.service<ProjectConfigurationImpl>().myState,
        userConfig = project.service<UserConfigurationImpl>().myState,
    )

    override var sdk: TinyGoSdk
        get() {
            val url = projectConfig.sdkUrl.ifEmpty { userConfig.sdk.homeUrl.ifEmpty { null } } ?: return nullSdk
            val knownVersion = service<TinyGoSdkList>().loadedSdks.firstOrNull { it.homeUrl == url }?.sdkVersion
            val version = knownVersion?.takeIf { it != unknownVersion } ?: userConfig.sdk.sdkVersion
            return TinyGoSdk(url, version)
        }
        set(value) {
            projectConfig.sdkUrl = value.homeUrl
            userConfig.sdk = value
        }

    override var cachedGoRoot: GoSdk
        get() = userConfig.cachedGoRoot
        set(value) {
            userConfig.cachedGoRoot = value
        }

    override fun saveState(project: Project) {
        GoUtil.cleanResolveCache(project)
        project.service<ProjectConfigurationImpl>().myState = projectConfig.copy()
        project.service<UserConfigurationImpl>().myState = userConfig.copy()
    }

    override fun modified(project: Project): Boolean {
        val currentSettings = TinyGoConfigurationImpl(project)
        return currentSettings.projectConfig != projectConfig || currentSettings.userConfig != userConfig
    }

    override fun deepCopy(): TinyGoConfigurationImpl {
        val projectConfigurationCopy = projectConfig.copy()
        val userConfigurationCopy = userConfig.copy()
        return TinyGoConfigurationImpl(
            projectConfig = projectConfigurationCopy,
            userConfig = userConfigurationCopy,
        )
    }

    override val enabled: Boolean
        get() = sdk != nullSdk
}

fun Project.tinyGoConfiguration(): TinyGoConfiguration = TinyGoConfiguration.getInstance(this)
