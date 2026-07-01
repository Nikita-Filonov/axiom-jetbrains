package io.github.nikitafilonov.axiom.psi

import com.goide.psi.GoFile
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoMethodDeclaration
import com.goide.psi.GoPointerType
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiDirectory
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker

/**
 * Recognises Axiom suite test methods in a Go PSI tree.
 *
 * A method qualifies as a runnable Axiom test if:
 *  1. Its name starts with `Test` (Axiom subtest naming convention).
 *  2. Its receiver type is passed to `axiom.NewSuite(...)` or
 *     `axiom.NewSuiteFactory(...)` somewhere in the same package.
 *
 * The detector is framework-agnostic: it does not care what the suite struct
 * embeds (`axiom.Suite`, `testsuite.BaseSuite`, custom wrappers, …) as long
 * as the struct is registered with Axiom.
 */
object AxiomSuiteDetector {

    /** Fast substring guard – files without this token are skipped without further parsing. */
    private const val AXIOM_MARKER = "NewSuite"

    /** Matches `new(TypeName)` inside `axiom.NewSuite(...)`. */
    private val RECEIVER_REGEX = Regex("""\bnew\(\s*(\w+)\s*\)""")

    /** Matches `func() *TypeName {` inside `axiom.NewSuiteFactory(...)`. */
    private val FACTORY_REGEX = Regex("""func\s*\(\s*\)\s*\*\s*(\w+)\s*\{""")

    private val TEST_FUNCS_KEY = Key.create<CachedValue<Map<String, String>>>("axiom.suite.testFuncs")

    /** True when [name] follows Go's test naming convention. */
    fun isTestMethodName(name: String): Boolean = name.startsWith("Test")

    /** Returns the receiver type name (without pointer prefix) or `null`. */
    fun receiverTypeName(method: GoMethodDeclaration): String? {
        val type = method.receiver?.type ?: return null
        val bare = if (type is GoPointerType) type.type ?: return null else type
        return bare.text?.removePrefix("*")?.trim()
    }

    /**
     * True when [method]'s receiver type is registered with `axiom.NewSuite`
     * in the containing package.
     */
    fun isAxiomSuiteMethod(method: GoMethodDeclaration): Boolean {
        val receiver = receiverTypeName(method) ?: return false
        val dir = method.containingFile.containingDirectory ?: return false
        return receiver in axiomRegistrations(dir)
    }

    /**
     * Name of the top-level `TestXxx` function in [method]'s package that
     * registers the receiver's struct with Axiom, or `null` if not found.
     */
    fun findRegisteringTestFunction(method: GoMethodDeclaration): String? {
        val receiver = receiverTypeName(method) ?: return null
        val dir = method.containingFile.containingDirectory ?: return null
        return axiomRegistrations(dir)[receiver]
    }

    /** Map: receiver type name → name of the TestXxx function that registers it. */
    private fun axiomRegistrations(dir: PsiDirectory): Map<String, String> =
        CachedValuesManager.getManager(dir.project).getCachedValue(dir, TEST_FUNCS_KEY, {
            val map = collectRegistrations(dir)
            CachedValueProvider.Result.create(map, PsiModificationTracker.MODIFICATION_COUNT)
        }, false)

    private fun collectRegistrations(dir: PsiDirectory): Map<String, String> = buildMap {
        for (file in dir.files) {
            val goFile = file as? GoFile ?: continue
            if (AXIOM_MARKER !in goFile.text) continue
            for (fn in goFile.functions) {
                val record = extractRegistration(fn) ?: continue
                putIfAbsent(record.first, record.second)
            }
        }
    }

    /**
     * If [fn] is a top-level `TestXxx` that calls `axiom.NewSuite(t, new(T), …)`
     * or `axiom.NewSuiteFactory(t, func() *T {…}, …)`, returns `T → funcName`.
     */
    private fun extractRegistration(fn: GoFunctionOrMethodDeclaration): Pair<String, String>? {
        val funcName = fn.name?.takeIf(::isTestMethodName) ?: return null
        val body = fn.block?.text ?: return null
        if (AXIOM_MARKER !in body) return null

        val receiver = RECEIVER_REGEX.find(body)?.groupValues?.get(1)
            ?: FACTORY_REGEX.find(body)?.groupValues?.get(1)
            ?: return null
        return receiver to funcName
    }
}
