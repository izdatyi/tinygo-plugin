package org.jetbrains.tinygoplugin.sdk

import com.goide.sdk.GoBasedSdk
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.intellij.util.io.URLUtil
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.warn
import org.jetbrains.tinygoplugin.icon.TinyGoPluginIcons
import java.io.File
import java.util.Objects
import javax.swing.Icon

@Suppress("ReturnCount", "MagicNumber")
fun tinyGoSdkVersion(versionString: String?): TinyGoSdkVersion {
    val logger = getLogger<TinyGoSdkVersion>()
    val numbers = versionString?.split('.')
    if (numbers == null) {
        logger.warn { "Null version provided" }
        return unknownVersion
    }
    if (numbers.size != 3) {
        logger.warn { "Could not parse version: $versionString" }
        return unknownVersion
    }
    val numbersParsed = numbers.mapNotNull { it.toIntOrNull() }
    if (numbersParsed.size != 3) {
        logger.warn { "Invalid version format: $versionString" }
        return unknownVersion
    }
    return TinyGoSdkVersion(numbersParsed[0], numbersParsed[1], numbersParsed[2])
}

val unknownVersion = TinyGoSdkVersion()

data class TinyGoSdkVersion(
    var major: Int = 0,
    var minor: Int = 0,
    var patch: Int = 0,
) {
    companion object {
        private const val MAX_VERSION = 1024
    }

    fun isAtLeast(version: TinyGoSdkVersion): Boolean = version != unknownVersion && version.toInt() <= toInt()

    fun isLessThan(version: TinyGoSdkVersion): Boolean = version != unknownVersion && version.toInt() > toInt()

    private fun toInt(): Int = patch + minor * MAX_VERSION + major * MAX_VERSION * MAX_VERSION

    override fun toString(): String = "$major.$minor.$patch"
}

private enum class TinyGoSdkValidity {
    UNKNOWN,
    VALID,
    INVALID,
}

@Suppress("TooManyFunctions")
open class TinyGoSdk(
    protected val tinyGoHomeUrl: String?,
    internal var sdkVersion: TinyGoSdkVersion = unknownVersion,
) : GoBasedSdk {
    constructor(tinyGoHomeUrl: String?, tinyGoVersion: String?) : this(
        tinyGoHomeUrl,
        tinyGoSdkVersion(tinyGoVersion),
    )

    @Volatile
    private var validity = if (tinyGoHomeUrl == null) TinyGoSdkValidity.INVALID else TinyGoSdkValidity.UNKNOWN

    @get:RequiresReadLock
    val sdkRoot: VirtualFile?
        get() = tinyGoHomeUrl?.let { VirtualFileManager.getInstance().findFileByUrl(it) }

    override fun getIcon(): Icon = TinyGoPluginIcons.TinyGoIcon

    override fun getVersion(): String = sdkVersion.toString()

    override fun getHomeUrl(): String = tinyGoHomeUrl ?: ""

    @RequiresReadLock
    override fun getSrcDir(): VirtualFile? = sdkRoot?.findChild("src")

    @RequiresReadLock
    override fun getExecutable(): VirtualFile? = osManager.executableVFile(sdkRoot)

    override fun isValid(): Boolean = validity == TinyGoSdkValidity.VALID

    @RequiresBackgroundThread
    fun refreshValidity(): Boolean {
        val homePath = urlToPath(tinyGoHomeUrl)
        val valid = homePath != null && checkDirectoryForTinyGo(File(homePath))
        validity = if (valid) TinyGoSdkValidity.VALID else TinyGoSdkValidity.INVALID
        return valid
    }

    override fun getName(): String = "TinyGo $version"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val sdk = other as TinyGoSdk
        return FileUtil.comparePaths(urlToPath(sdk.tinyGoHomeUrl), urlToPath(tinyGoHomeUrl)) == 0
    }

    override fun hashCode(): Int = Objects.hash(tinyGoHomeUrl)
}

internal fun urlToPath(url: String?): String? = url?.let { URLUtil.urlToPath(it) }

const val TINY_GO_VERSION_REGEX = """tinygo version (\d+.\d+.\d+)"""

@Suppress("UnusedParameter")
suspend fun TinyGoSdk.computeVersion(project: Project? = null): Boolean {
    val homePath = urlToPath(homeUrl) ?: return false
    val exeFile =
        File(homePath, "bin/tinygo.exe").takeIf { it.exists() }
            ?: File(homePath, "bin/tinygo").takeIf { it.exists() }
            ?: return false
    return try {
        val process =
            ProcessBuilder(exeFile.absolutePath, "version")
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)
        val match = TINY_GO_VERSION_REGEX.toRegex().find(output)
        if (match != null) {
            sdkVersion = tinyGoSdkVersion(match.groupValues[1])
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}

val nullSdk = TinyGoSdk(null, unknownVersion)
