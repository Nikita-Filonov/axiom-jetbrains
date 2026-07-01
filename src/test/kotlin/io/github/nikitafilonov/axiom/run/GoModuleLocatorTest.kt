package io.github.nikitafilonov.axiom.run

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Pure JUnit tests – no IntelliJ Platform runtime required, so this suite
 * runs in a fraction of a second and can safely be executed on every save.
 */
class GoModuleLocatorTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `readModulePath returns the module directive from go_mod`() {
        val root = tmp.newFolder("proj")
        root.resolve("go.mod").writeText(
            """
            module gitlab.example.com/team/proj

            go 1.22

            require github.com/foo/bar v1.2.3
            """.trimIndent()
        )

        assertEquals("gitlab.example.com/team/proj", GoModuleLocator.readModulePath(root.absolutePath))
    }

    @Test
    fun `readModulePath returns null when go_mod is missing`() {
        val root = tmp.newFolder("empty")
        assertNull(GoModuleLocator.readModulePath(root.absolutePath))
    }

    @Test
    fun `readModulePath trims leading spaces after 'module '`() {
        val root = tmp.newFolder("proj")
        root.resolve("go.mod").writeText("module   github.com/spaced/module   \n")
        assertEquals("github.com/spaced/module", GoModuleLocator.readModulePath(root.absolutePath))
    }
}
