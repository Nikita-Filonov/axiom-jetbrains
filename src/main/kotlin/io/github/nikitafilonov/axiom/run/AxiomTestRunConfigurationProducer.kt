package io.github.nikitafilonov.axiom.run

import com.goide.execution.testing.GoTestRunConfiguration
import com.goide.execution.testing.GoTestRunConfigurationType
import com.goide.psi.GoMethodDeclaration
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Teaches IntelliJ how to build a Go test run configuration for an Axiom
 * suite subtest.
 *
 * By registering this producer we get, for free, every standard executor action
 * (Run, Debug, Run with Coverage, Profile with CPU/Memory/Blocking, Modify
 * Run Configuration…) via [com.intellij.execution.lineMarker.ExecutorAction.getActions].
 *
 * The producer is intentionally *lazy* – GoLand only asks for a configuration
 * factory when an executor action is actually invoked.
 */
class AxiomTestRunConfigurationProducer : LazyRunConfigurationProducer<GoTestRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory =
        GoTestRunConfigurationType.getInstance().configurationFactories.first()

    /**
     * Runs when the user invokes any executor action from the gutter (or
     * "Run ..." context menu). We locate an Axiom target above [sourceElement]
     * and hand the config over to [GoTestConfigurator].
     */
    override fun setupConfigurationFromContext(
        configuration: GoTestRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val method = findAxiomMethod(context) ?: return false
        val target = AxiomTestTarget.from(method) ?: return false
        if (!GoTestConfigurator.apply(configuration, target)) return false

        configuration.name = target.displayName
        sourceElement.set(method.nameIdentifier ?: method)
        return true
    }

    /**
     * Used by IntelliJ to decide whether an existing configuration can be
     * reused for the current context instead of creating a new one.
     */
    override fun isConfigurationFromContext(
        configuration: GoTestRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val method = findAxiomMethod(context) ?: return false
        val target = AxiomTestTarget.from(method) ?: return false
        return configuration.pattern == target.runPattern
    }

    /** Walks the context PSI upward to the enclosing Axiom suite method, if any. */
    private fun findAxiomMethod(context: ConfigurationContext): GoMethodDeclaration? {
        val leaf = context.psiLocation ?: return null
        val method = PsiTreeUtil.getParentOfType(leaf, GoMethodDeclaration::class.java, false)
            ?: return null
        return method.takeIf { AxiomTestTarget.from(it) != null }
    }
}
