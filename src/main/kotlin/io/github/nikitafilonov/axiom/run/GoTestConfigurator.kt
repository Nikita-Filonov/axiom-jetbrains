package io.github.nikitafilonov.axiom.run

import com.goide.execution.testing.GoTestRunConfiguration
import com.intellij.openapi.diagnostic.logger

/**
 * Encapsulates the (potentially version-dependent) knowledge of how to shape a
 * [GoTestRunConfiguration] so that Axiom subtests can be Run / Debug / Coverage /
 * Profile just like native Go tests.
 *
 * The Go plugin's API surface is not part of the stable Platform SDK, so this
 * file is the single place we have to update when JetBrains rename a setter.
 * Everywhere else in the plugin uses [GoTestConfigurator] and stays version-safe.
 */
object GoTestConfigurator {

    private val LOG = logger<GoTestConfigurator>()

    /**
     * Configures [config] to run the given [target] as an isolated Axiom subtest.
     *
     * Uses `Kind.PACKAGE` (required for debugger support – `DIRECTORY` kind
     * cannot be debugged) plus a `-run` pattern anchored on both sides.
     *
     * Returns `true` on success, `false` if any critical property could not be
     * set – callers can then either abort or fall back to a plain-console run.
     */
    fun apply(config: GoTestRunConfiguration, target: AxiomTestTarget): Boolean {
        return try {
            setKindToPackage(config)
            setPackagePath(config, target.packageImportPath)
            config.pattern = target.runPattern
            config.workingDirectory = target.workingDirectory
            true
        } catch (e: NoSuchMethodError) {
            LOG.warn("Go plugin API mismatch – GoTestRunConfiguration is missing an expected member", e)
            false
        } catch (e: Throwable) {
            LOG.warn("Failed to configure GoTestRunConfiguration for ${target.displayName}", e)
            false
        }
    }

    /**
     * The [GoTestRunConfiguration.Kind] enum is nested and Java-only in some
     * versions; using reflection here shields us from repackaging.
     */
    private fun setKindToPackage(config: GoTestRunConfiguration) {
        val setKind = config.javaClass.methods.firstOrNull {
            it.name == "setKind" && it.parameterCount == 1
        } ?: return
        val packageKind = setKind.parameterTypes[0].enumConstants
            ?.firstOrNull { (it as Enum<*>).name == "PACKAGE" } ?: return
        setKind.invoke(config, packageKind)
    }

    /**
     * Historically named either `setPackagePath` (older builds) or `setPackage`
     * (newer). Try each; return silently if none exist – the config will still
     * launch, just against the wider workspace instead of a single package.
     */
    private fun setPackagePath(config: GoTestRunConfiguration, importPath: String) {
        val setter = config.javaClass.methods.firstOrNull { m ->
            m.parameterCount == 1 &&
                m.parameterTypes[0] == String::class.java &&
                (m.name == "setPackagePath" || m.name == "setPackage" || m.name == "setImportPath")
        } ?: return
        setter.invoke(config, importPath)
    }
}
