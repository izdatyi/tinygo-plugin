package org.jetbrains.tinygoplugin.sdk

import com.goide.GoNotifications
import com.goide.sdk.GoSdkService
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.TinyGoBundle
import org.jetbrains.tinygoplugin.services.TinyGoSettingsService
import java.io.File

const val CONFIGURATION_INCOMPLETE_NOTIFICATION = "notifications.tinygoSDK.configuration.incomplete"

fun notifyTinyGoNotConfigured(
    project: Project?,
    content: String,
) {
    val notification =
        GoNotifications
            .getGeneralGroup()
            .createNotification(
                TinyGoBundle.message(CONFIGURATION_INCOMPLETE_NOTIFICATION),
                content,
                NotificationType.WARNING,
            )
    notification.addAction(
        object : NotificationAction("TinyGo settings") {
            override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

            override fun actionPerformed(
                e: AnActionEvent,
                notification: Notification,
            ) {
                if (project == null) return
                service<ShowSettingsUtil>().editConfigurable(project, TinyGoSettingsService(project))
            }
        },
    )
    notification.notify(project)
}

fun suggestSdkDirectoryStr(): String = suggestSdkDirectory()?.canonicalPath ?: ""

fun suggestSdkDirectories(): Collection<File> =
    osManager
        .suggestedDirectories()
        .asSequence()
        .map { File(it) }
        .filter(File::exists)
        .filter(::checkBin)
        .filter(::checkTargets)
        .filter(::checkMachinesSources)
        .distinctBy { it.canonicalPath }
        .toList()

fun findTinyGoInPath(): File? {
    val tinyGoExec =
        PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS(osManager.executableBaseName()) ?: return null
    return tinyGoExec.parentFile?.parentFile
}

fun suggestSdkDirectory(): File? {
    val tinyGoPath = findTinyGoInPath()
    val isValid =
        tinyGoPath != null &&
            checkBin(tinyGoPath) &&
            checkTargets(tinyGoPath) &&
            checkMachinesSources(File(tinyGoPath, "src"))
    if (isValid) {
        return tinyGoPath
    }
    return suggestSdkDirectories().firstOrNull()
}

fun createTinyGoEnvironment(
    project: Project,
    sdkRoot: VirtualFile?,
    customEnv: Map<String, String> = emptyMap(),
): Map<String, String> {
    val env = mutableMapOf<String, String>()
    env.putAll(customEnv)

    var goBinDir: String? = null
    val goSdk = GoSdkService.getInstance(project).getSdk(null)
    val goHome =
        if (goSdk.isValid) {
            goSdk.homeUrl.let {
                com.intellij.openapi.vfs.VfsUtil
                    .urlToPath(it)
            }
        } else {
            ""
        }
    if (goHome.isNotEmpty()) {
        val goHomeFile = File(goHome)
        env["GOROOT"] = goHomeFile.absolutePath
        val bin = File(goHomeFile, "bin")
        if (bin.exists()) {
            goBinDir = bin.absolutePath
        }
    }

    var tinyGoBinDir: String? = null
    if (sdkRoot != null && sdkRoot.isValid) {
        val tinyGoHome = sdkRoot.canonicalPath ?: sdkRoot.path
        val tinyGoHomeFile = File(tinyGoHome)
        env["TINYGOROOT"] = tinyGoHomeFile.absolutePath
        val bin = File(tinyGoHomeFile, "bin")
        if (bin.exists()) {
            tinyGoBinDir = bin.absolutePath
        }
    }

    val currentPath = System.getenv("PATH") ?: System.getenv("Path") ?: ""
    val pathParts = listOfNotNull(tinyGoBinDir, goBinDir, currentPath.ifEmpty { null })
    val newPath = pathParts.joinToString(File.pathSeparator)
    env["PATH"] = newPath
    env["Path"] = newPath

    return env
}
