package org.jetbrains.tinygoplugin.configuration

import com.goide.sdk.GoSdk
import com.goide.util.GoUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.tinygoplugin.sdk.TinyGoSdk
import org.jetbrains.tinygoplugin.sdk.nullSdk
import java.io.File

interface TinyGoConfiguration : UserConfiguration, ProjectConfiguration {
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
) : TinyGoConfiguration, UserConfiguration by userConfig, ProjectConfiguration by projectConfig {

    constructor(project: Project) : this(
        projectConfig = project.service<ProjectConfigurationImpl>().myState,
        userConfig = project.service<UserConfigurationImpl>().myState,
    )

    override var sdk: TinyGoSdk
        get() {
            val url = projectConfig.sdkUrl.ifEmpty { userConfig.sdk.homeUrl.ifEmpty { null } } ?: return nullSdk
            val ver = projectConfig.sdkVersion.ifEmpty { userConfig.sdk.version }
            return TinyGoSdk(url, ver.ifEmpty { null })
        }
        set(value) {
            projectConfig.sdkUrl = value.homeUrl
            projectConfig.sdkVersion = value.version
            userConfig.sdk = value
        }

    override var cachedGoRoot: GoSdk
        get() {
            val url = projectConfig.cachedGoRootUrl.ifEmpty { userConfig.cachedGoRoot.homeUrl.ifEmpty { null } }
            if (url != null) {
                val sdk = GoSdk.fromUrl(url)
                if (sdk != GoSdk.NULL && sdk.srcDir != null) return sdk
            }
            val fallback = findFallbackCachedGoRoot()
            if (fallback != null) {
                projectConfig.cachedGoRootUrl = fallback.homeUrl
                return fallback
            }
            return userConfig.cachedGoRoot
        }
        set(value) {
            projectConfig.cachedGoRootUrl = value.homeUrl
            userConfig.cachedGoRoot = value
        }

    private fun findFallbackCachedGoRoot(): GoSdk? {
        val app = com.intellij.openapi.application.ApplicationManager.getApplication()
        if (app != null && app.isUnitTestMode) return null
        val localAppData = System.getenv("LOCALAPPDATA") ?: ""
        val tinygoDir = if (localAppData.isNotEmpty()) File(localAppData, "tinygo") else null
        val latestGoroot = if (tinygoDir != null && tinygoDir.isDirectory) {
            tinygoDir.listFiles { f -> f.isDirectory && f.name.startsWith("goroot-") }
                ?.maxByOrNull { it.lastModified() }
        } else {
            null
        }
        val gorootPath = latestGoroot?.absolutePath ?: return null
        val sdk = GoSdk.fromUrl(VfsUtil.pathToUrl(gorootPath))
        return if (sdk != GoSdk.NULL && sdk.srcDir != null) sdk else null
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
