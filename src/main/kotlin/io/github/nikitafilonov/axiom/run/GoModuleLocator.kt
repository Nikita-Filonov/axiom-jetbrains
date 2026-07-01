package io.github.nikitafilonov.axiom.run

import com.goide.psi.GoFile
import com.intellij.openapi.vfs.VirtualFile

/**
 * Utilities for locating the Go module root and computing package import paths.
 *
 * Kept as a dedicated object so the (potentially version-specific) Go plugin
 * API surface used here is easy to swap or mock.
 *
 * All lookups go through the Virtual File System, so the object works
 * uniformly against on-disk projects, in-memory test fixtures and remote
 * workspaces.
 */
object GoModuleLocator {

    /**
     * Walks up from [file]'s directory looking for a `go.mod` file.
     * Returns the absolute path of the directory that contains `go.mod`,
     * or `null` if no module root is found.
     */
    fun findGoModDir(file: GoFile): String? =
        findGoMod(file)?.parent?.path

    /** Same as [findGoModDir] but starting from an explicit [VirtualFile]. */
    fun findGoModDir(start: VirtualFile): String? =
        findGoMod(start)?.parent?.path

    /** Returns the `go.mod` [VirtualFile] closest to [file], or `null`. */
    fun findGoMod(file: GoFile): VirtualFile? {
        val start = file.virtualFile?.parent ?: return null
        return findGoMod(start)
    }

    /** Walks up from [start] and returns the first `go.mod` [VirtualFile] found. */
    fun findGoMod(start: VirtualFile): VirtualFile? {
        var dir: VirtualFile? = if (start.isDirectory) start else start.parent
        while (dir != null) {
            dir.findChild("go.mod")?.let { return it }
            dir = dir.parent
        }
        return null
    }

    /**
     * Reads the `module` directive from [workDir]'s `go.mod` on disk.
     * Convenience overload used in pure-JUnit tests and diagnostic tooling.
     */
    fun readModulePath(workDir: String): String? =
        java.io.File(workDir, "go.mod")
            .takeIf { it.exists() }
            ?.readLines()
            ?.let(::parseModuleDirective)

    /**
     * Reads the `module` directive from a `go.mod` [VirtualFile].
     *
     * This is the variant used at runtime: it goes through the VFS and
     * therefore works with in-memory test fixtures as well as real files.
     */
    fun readModulePath(goMod: VirtualFile): String? =
        goMod.takeIf { !it.isDirectory }
            ?.let { String(it.contentsToByteArray(), Charsets.UTF_8).lines() }
            ?.let(::parseModuleDirective)

    private fun parseModuleDirective(lines: List<String>): String? =
        lines.firstOrNull { it.startsWith("module ") }
            ?.removePrefix("module ")
            ?.trim()
}
