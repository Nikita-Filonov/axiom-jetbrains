package io.github.nikitafilonov.axiom.psi

import com.goide.psi.GoFile
import com.goide.psi.GoMethodDeclaration
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Verifies the pure-PSI detection rules on real Go fixtures.
 *
 * Uses [BasePlatformTestCase] to spin up a lightweight IDE with the Go
 * plugin loaded so we get authentic [GoFile]/[GoMethodDeclaration] trees.
 */
class AxiomSuiteDetectorTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData/axiom"

    fun `test detects a NewSuite receiver and its registering func`() {
        val method = loadMethod("newSuite/suite_test.go", "TestGetCard")

        assertTrue("expected suite method to be recognised", AxiomSuiteDetector.isAxiomSuiteMethod(method))
        assertEquals("TestCardServiceSuite", AxiomSuiteDetector.findRegisteringTestFunction(method))
        assertEquals("serviceSuite", AxiomSuiteDetector.receiverTypeName(method))
    }

    fun `test detects a NewSuiteFactory receiver`() {
        val method = loadMethod("newSuiteFactory/suite_test.go", "TestGetAccount")

        assertTrue(AxiomSuiteDetector.isAxiomSuiteMethod(method))
        assertEquals("TestAccountServiceSuite", AxiomSuiteDetector.findRegisteringTestFunction(method))
    }

    fun `test recognises framework-agnostic base suites`() {
        val method = loadMethod("customBaseSuite/suite_test.go", "TestSomething")

        assertTrue(
            "detector must ignore what the struct embeds and only look at NewSuite calls",
            AxiomSuiteDetector.isAxiomSuiteMethod(method),
        )
    }

    fun `test rejects testify suites`() {
        val method = loadMethod("notAxiom/suite_test.go", "TestNothing")

        assertFalse(AxiomSuiteDetector.isAxiomSuiteMethod(method))
        assertNull(AxiomSuiteDetector.findRegisteringTestFunction(method))
    }

    fun `test ignores non-Test helper methods`() {
        val method = loadMethod("newSuite/suite_test.go", "loadFixture")

        assertFalse(AxiomSuiteDetector.isTestMethodName(method.name!!))
        // detector.isAxiomSuiteMethod does not filter by name – that's the
        // contributor's job – but it still returns true because the receiver
        // is a registered suite. Documented behaviour.
        assertTrue(AxiomSuiteDetector.isAxiomSuiteMethod(method))
    }

    fun `test isTestMethodName follows Go convention`() {
        assertTrue(AxiomSuiteDetector.isTestMethodName("TestFoo"))
        assertTrue(AxiomSuiteDetector.isTestMethodName("Test"))
        assertFalse(AxiomSuiteDetector.isTestMethodName("testFoo"))
        assertFalse(AxiomSuiteDetector.isTestMethodName("BenchmarkFoo"))
        assertFalse(AxiomSuiteDetector.isTestMethodName(""))
    }

    /** Loads a fixture file and returns its method named [methodName]. */
    private fun loadMethod(fixturePath: String, methodName: String): GoMethodDeclaration {
        val psiFile = myFixture.configureByFile(fixturePath) as GoFile
        return PsiTreeUtil.findChildrenOfType(psiFile, GoMethodDeclaration::class.java)
            .firstOrNull { it.name == methodName }
            ?: error("Method $methodName not found in $fixturePath")
    }
}
