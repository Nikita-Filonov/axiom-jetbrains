package io.github.nikitafilonov.axiom.gutter

import com.goide.psi.GoMethodDeclaration
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.github.nikitafilonov.axiom.run.AxiomTestTarget

/**
 * Draws the green run gutter icon on every Axiom suite test method.
 *
 * Actions are pulled from [ExecutorAction.getActions] so that the popup
 * contains the full native GoLand menu (Run / Debug / Coverage / Profile /
 * Modify Run Configuration…). The actual configuration is produced by
 * `AxiomTestRunConfigurationProducer`.
 */
class AxiomSuiteRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        val method = enclosingMethod(element) ?: return null
        if (!isAnchoredOnMethodName(element, method)) return null

        val target = AxiomTestTarget.from(method) ?: return null

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            ExecutorAction.getActions(0),
        ) { "Run ${target.displayName}" }
    }

    /**
     * The line-marker framework calls us on every leaf token; we only want to
     * fire once per method – on the identifier that IS the method name.
     */
    private fun isAnchoredOnMethodName(element: PsiElement, method: GoMethodDeclaration): Boolean {
        if (element !is LeafPsiElement) return false
        val name = method.name ?: return false
        return element.text == name
    }

    private fun enclosingMethod(element: PsiElement): GoMethodDeclaration? =
        PsiTreeUtil.getParentOfType(element, GoMethodDeclaration::class.java, false)
}
