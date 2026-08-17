package org.jetbrains.tinygoplugin.services

import com.goide.project.GoRootsProvider
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.tinygoplugin.configuration.tinyGoConfiguration
import java.util.Collections

class TinyGoRootsProvider : GoRootsProvider {

    override fun getGoPathRoots(p0: Project?, p1: Module?): MutableCollection<VirtualFile> = Collections.emptyList()

    override fun getGoPathSourcesRoots(p0: Project?, p1: Module?): MutableCollection<VirtualFile> =
        Collections.emptyList()

    override fun getGoPathBinRoots(p0: Project?, p1: Module?): MutableCollection<VirtualFile> = Collections.emptyList()

    override fun isExternal(): Boolean = false
    @Suppress("ReturnCount")
    override fun getVendorDirectoriesInResolveScope(
        project: Project,
        module: Module?,
        file: VirtualFile?,
    ): MutableCollection<VirtualFile>? {
        if (file == null || module == null || !isTinyGoActive(project, module)) {
            return null
        }
        val cachedGoRoot = project.tinyGoConfiguration().cachedGoRoot
        val cachedGoRootDir = cachedGoRoot.sdkRoot ?: return null
        if (VfsUtil.isAncestor(cachedGoRootDir, file, false)) {
            return mutableListOf(file.parent)
        }
        return null
    }
}
