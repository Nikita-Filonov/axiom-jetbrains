package io.github.nikitafilonov.axiom.gutter

import com.goide.psi.GoFile
import com.goide.psi.GoMethodDeclaration
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * End-to-end tests: given a fixture Go file, verify that our contributor
 * produces a gutter Info on the right leaves and nothing elsewhere.
 *
 * We drive the contributor directly (`contributor.getInfo(leaf)`) rather than
 * relying on `myFixture.doHighlighting()`, because the gutter API in some
 * platform versions runs contributors on background threads and we want the
 * assertions to be synchronous.
 */
class AxiomSuiteRunLineMarkerContributorTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData/axiom"

    private val contributor = AxiomSuiteRunLineMarkerContributor()

    fun `test marks TestXxx methods of an Axiom NewSuite receiver`() {
        val file = configureFixture("newSuite")

        assertHasMarker(file, "TestGetCard")
        assertHasMarker(file, "TestListCards")
    }

    fun `test marks TestXxx methods of an Axiom NewSuiteFactory receiver`() {
        val file = configureFixture("newSuiteFactory")
        assertHasMarker(file, "TestGetAccount")
    }

    fun `test does not mark helper methods without the Test prefix`() {
        val file = configureFixture("newSuite")
        assertNoMarker(file, "loadFixture")
    }

    fun `test does not mark testify suite methods`() {
        val file = configureFixture("notAxiom")
        assertNoMarker(file, "TestNothing")
    }

    fun `test recognises framework-agnostic base suites`() {
        val file = configureFixture("customBaseSuite")
        assertHasMarker(file, "TestSomething")
    }

    /**
     * Copies the whole fixture directory (Go source + `go.mod`) into the
     * in-memory project and returns the configured [GoFile] for `suite_test.go`.
     *
     * We need the sibling `go.mod` so that [GoModuleLocator] can resolve the
     * module root – without it every marker computation short-circuits to null.
     */
    private fun configureFixture(fixtureDir: String): GoFile {
        myFixture.copyDirectoryToProject(fixtureDir, fixtureDir)
        return myFixture.configureFromTempProjectFile("$fixtureDir/suite_test.go") as GoFile
    }

    /** Asserts that the contributor produces an Info on the identifier leaf of [methodName]. */
    private fun assertHasMarker(file: GoFile, methodName: String) {
        val leaf = methodNameLeaf(file, methodName)
        assertNotNull(
            "expected a gutter marker on method '$methodName'",
            contributor.getInfo(leaf),
        )
    }

    private fun assertNoMarker(file: GoFile, methodName: String) {
        val leaf = methodNameLeaf(file, methodName)
        assertNull(
            "did not expect a gutter marker on method '$methodName'",
            contributor.getInfo(leaf),
        )
    }

    /** Finds the leaf PSI element that carries the method's identifier text. */
    private fun methodNameLeaf(file: GoFile, methodName: String): PsiElement {
        val method = PsiTreeUtil.findChildrenOfType(file, GoMethodDeclaration::class.java)
            .firstOrNull { it.name == methodName }
            ?: error("method $methodName not found")
        val id = method.nameIdentifier ?: error("nameIdentifier missing on $methodName")
        // Walk down to the first LEAF whose text equals the method name.
        // Handles both cases: identifier IS a leaf, or wraps one.
        return generateSequence<PsiElement>(id) { it.firstChild }
            .firstOrNull { it is LeafPsiElement && it.getText() == methodName }
            ?: error("no matching leaf under identifier of $methodName")
    }
}
