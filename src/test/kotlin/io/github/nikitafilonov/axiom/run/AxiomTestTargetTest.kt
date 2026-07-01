package io.github.nikitafilonov.axiom.run

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pure JUnit tests for the deterministic (non-PSI) side of [AxiomTestTarget].
 *
 * Full PSI-based construction via [AxiomTestTarget.from] is covered by the
 * platform-integration test in `AxiomTestTargetIntegrationTest`.
 */
class AxiomTestTargetTest {

    private val target = AxiomTestTarget(
        suiteFuncName = "TestFooSuite",
        methodName = "TestBar",
        packageImportPath = "github.com/example/foo/pkg/bar",
        workingDirectory = "/tmp/foo",
    )

    @Test
    fun `runPattern is anchored on both suite name and method name`() {
        // Prevents `TestBar` from matching `TestBarBaz` at go-test time.
        assertEquals("^TestFooSuite$/^TestBar$", target.runPattern)
    }

    @Test
    fun `displayName combines suite and method separated by a slash`() {
        assertEquals("TestFooSuite/TestBar", target.displayName)
    }
}
