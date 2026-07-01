package io.github.nikitafilonov.axiom.run

import com.goide.psi.GoFile
import com.goide.psi.GoMethodDeclaration
import io.github.nikitafilonov.axiom.psi.AxiomSuiteDetector

/**
 * A fully-resolved Axiom test target that is ready to be turned into a
 * `GoTestRunConfiguration`.
 *
 * @property suiteFuncName Top-level Go test function that registers the suite (e.g. `TestFooSuite`).
 * @property methodName    The `TestXxx` method on the suite struct to execute.
 * @property packageImportPath Full Go import path of the containing package.
 * @property workingDirectory Absolute path to the module root (containing `go.mod`).
 */
data class AxiomTestTarget(
    val suiteFuncName: String,
    val methodName: String,
    val packageImportPath: String,
    val workingDirectory: String,
) {
    /**
     * Pattern for `go test -run`. Anchored on both sides so it matches the
     * subtest exactly and does not pick up neighbours like
     * `TestFoo` vs `TestFooNegative`.
     */
    val runPattern: String get() = "^$suiteFuncName$/^$methodName$"

    /** Human-readable label for run-configuration titles and tooltips. */
    val displayName: String get() = "$suiteFuncName/$methodName"

    companion object {
        /**
         * Builds a target from [method] or returns `null` when the method is
         * not a runnable Axiom suite test.
         *
         * All checks are cheap and safe to call from EDT / read actions.
         */
        fun from(method: GoMethodDeclaration): AxiomTestTarget? {
            val methodName = method.name ?: return null
            if (!AxiomSuiteDetector.isTestMethodName(methodName)) return null
            if (!AxiomSuiteDetector.isAxiomSuiteMethod(method)) return null

            val suiteFunc = AxiomSuiteDetector.findRegisteringTestFunction(method) ?: return null
            val goFile = method.containingFile as? GoFile ?: return null

            val workDir = GoModuleLocator.findGoModDir(goFile) ?: return null
            val importPath = resolveImportPath(goFile, workDir) ?: return null

            return AxiomTestTarget(
                suiteFuncName = suiteFunc,
                methodName = methodName,
                packageImportPath = importPath,
                workingDirectory = workDir,
            )
        }

        /**
         * Import path of [file]'s package.
         *
         * Prefers Go plugin's own [GoFile.getImportPath] (which correctly
         * handles vendored/replace directives and multi-module workspaces).
         * Falls back to `<module>/<relPath>` computed from `go.mod` when the
         * plugin returns nothing.
         */
        private fun resolveImportPath(file: GoFile, workDir: String): String? {
            file.getImportPath(false)?.takeIf { it.isNotBlank() }?.let { return it }

            val filePath = file.virtualFile?.parent?.path ?: return null
            val modulePath = GoModuleLocator.readModulePath(workDir) ?: return null
            val relPath = filePath.removePrefix(workDir).trim('/')
            return if (relPath.isEmpty()) modulePath else "$modulePath/$relPath"
        }
    }
}
