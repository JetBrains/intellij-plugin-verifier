package com.jetbrains.pluginverifier.tests.filter

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.filtering.KeepOnlyCondition
import com.jetbrains.pluginverifier.filtering.KeepOnlyProblemsFilter
import com.jetbrains.pluginverifier.tests.VerificationRunner
import com.jetbrains.pluginverifier.tests.findMockIdePath
import com.jetbrains.pluginverifier.tests.findMockPluginJarPath
import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test

class ProblemFilterTest {
    companion object {
        lateinit var verificationRunner: VerificationRunner
        lateinit var ide: Ide
        lateinit var plugin: IdePlugin

        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            prepareTestSystemProperties()
            val idePath = findMockIdePath()
            val pluginFile = findMockPluginJarPath()

            ide = IdeManager.createManager().createIde(idePath)
            plugin = (IdePluginManager.createManager().createPlugin(pluginFile) as PluginCreationSuccess).plugin
            verificationRunner = VerificationRunner()
        }

        private fun prepareTestSystemProperties() {
            System.setProperty("plugin.verifier.test.private.interface.method.name", "privateInterfaceMethodTestName")
        }
    }

    @Test
    fun `check only problems matching keep-only-problems filter found`() {
        val filterRegex = ".*kotlin.*".toRegex()
        val keepOnlyProblemsFilter = listOf(KeepOnlyProblemsFilter(listOf(KeepOnlyCondition(null, null, filterRegex))))
        val verificationResult = verificationRunner.runPluginVerification(
            ide,
            plugin,
            keepOnlyProblemsFilter
        ) as PluginVerificationResult.Verified

        val compProblemsDescriptions = verificationResult.compatibilityProblems.map { it.shortDescription }

        assertArrayEquals("Compatibility problems by keep-only-filter '$filterRegex' are not as expected",
            listOf(
                "Abstract method defaults.kotlin.I.noDefault() : int is not implemented",
                "Invocation of unresolved method kotlinDefault.KotlinDefault.bar\$default(KotlinDefault, int, int, Object) : void",
                "Invocation of unresolved method kotlinDefault.KotlinDefault.bar(int) : void",
                "Invocation of unresolved method kotlinDefault.KotlinDefault.foo\$default(KotlinDefault, int, int, Object) : void",
                "Invocation of unresolved method kotlinDefault.KotlinDefault.foo(int) : void"
            ).toTypedArray(),
            compProblemsDescriptions.sorted().toTypedArray()
        )
    }
}