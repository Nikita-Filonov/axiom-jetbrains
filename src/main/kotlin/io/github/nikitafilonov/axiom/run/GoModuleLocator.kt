package io.github.nikitafilonov.axiom.run

import com.goide.psi.GoFile
import com.intellij.openapi.vfs.VirtualFile

/**
 * Utilities for locating the Go module root and computing package import paths.
 *
 * Kept as a dedicated object so the (potentially version-specific) Go plugin
 * API surface used here is easy to swap or mock.
 */
object GoModuleLocator {

    /**
     * Walks up from [file]'s directory looking for a `go.mod` file.
     * Returns the absolute path of the directory that contains `go.mod`,
     * or `null` if no module root is found.
     */
    fun findGoModDir(file: GoFile): String? {
        val start = file.virtualFile?.parent ?: return null
        return findGoModDir(start)
    }

    /** Same as [findGoModDir] but starting from an explicit [VirtualFile]. */
    fun findGoModDir(start: VirtualFile): String? {
        var dir: VirtualFile? = if (start.isDirectory) start else start.parent
        while (dir != null) {
            if (dir.findChild("go.mod") != null) return dir.path
            dir = dir.parent
        }
        return null
    }

    /**
     * Reads the `module` directive from [workDir]'s `go.mod`.
     * Used only as a fallback when [GoFile.getImportPath] is unavailable.
     */
    fun readModulePath(workDir: String): String? =
        java.io.File(workDir, "go.mod")
            .takeIf { it.exists() }
            ?.readLines()
            ?.firstOrNull { it.startsWith("module ") }
            ?.removePrefix("module ")
            ?.trim()
}
