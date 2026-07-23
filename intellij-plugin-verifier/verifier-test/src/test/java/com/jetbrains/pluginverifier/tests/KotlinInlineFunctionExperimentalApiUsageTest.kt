package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tests.bytecode.DirectExperimentalCallerDump
import com.jetbrains.pluginverifier.tests.bytecode.ExperimentalInlinedCallerDump
import com.jetbrains.pluginverifier.tests.bytecode.ExperimentalProgressKtDump
import com.jetbrains.pluginverifier.tests.bytecode.ExperimentalReporterHandleDump
import com.jetbrains.pluginverifier.tests.bytecode.OwnExperimentalInlineCallerDump
import com.jetbrains.pluginverifier.tests.bytecode.PluginExperimentalUtilsKtDump
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalApiUsage
import com.jetbrains.pluginverifier.usages.experimental.ExperimentalMethodUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Companion to [KotlinInlineFunctionInternalApiUsageTest] (MP-7133): `ExperimentalApiUsageProcessor`
 * did not call `KotlinInlinedCodeDetector` at all, so `@ApiStatus.Experimental` usages inlined from
 * a public SDK `inline fun` were still attributed to the plugin even after the MP-7133 fix, unlike
 * `@ApiStatus.Internal`/`@ApiStatus.OverrideOnly` usages. The mimicked sources are quoted in each
 * dump's KDoc, see [ExperimentalInlinedCallerDump].
 */
class KotlinInlineFunctionExperimentalApiUsageTest : BaseBytecodeTest() {

  private val thirdPartyPluginSpec = IdeaPluginSpec("com.example.progress", "Third Party Inc.")

  @Test
  fun `experimental API referenced only from the inlined body of a public SDK inline function is not reported`() {
    val idePlugin = buildIdePlugin(thirdPartyPluginSpec) {
      dirs("com/example/plugin") {
        file("MyExperimentalAction.class", ExperimentalInlinedCallerDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertEquals(emptySet<CompatibilityProblem>(), verificationResult.compatibilityProblems)
    assertEquals(
      "Experimental API usages coming from an inlined body of a public SDK inline function " +
        "must not be attributed to the plugin",
      emptySet<ExperimentalApiUsage>(),
      verificationResult.experimentalApiUsages
    )
  }

  @Test
  fun `experimental API inlined from the plugin's own inline function is still reported`() {
    val idePlugin = buildIdePlugin(thirdPartyPluginSpec) {
      dirs("com/example/plugin") {
        file("OwnExperimentalCaller.class", OwnExperimentalInlineCallerDump.dump())
        file("ExperimentalUtilsKt.class", PluginExperimentalUtilsKtDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertTrue(
      "Experimental API usage inlined from the plugin's own inline function is the plugin author's " +
        "own code and must still be reported",
      verificationResult.experimentalApiUsages
        .filterIsInstance<ExperimentalMethodUsage>()
        .any { "experimentalCurrentStep" in it.fullDescription }
    )
  }

  @Test
  fun `experimental API invoked directly by plugin code is reported`() {
    val idePlugin = buildIdePlugin(thirdPartyPluginSpec) {
      dirs("com/example/plugin") {
        file("DirectExperimentalUsage.class", DirectExperimentalCallerDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertEquals(emptySet<CompatibilityProblem>(), verificationResult.compatibilityProblems)
    assertTrue(
      "Direct invocation of an experimental API method must be reported",
      verificationResult.experimentalApiUsages
        .filterIsInstance<ExperimentalMethodUsage>()
        .any { "experimentalCurrentStep" in it.fullDescription }
    )
  }

  private fun runVerification(idePlugin: IdePlugin): PluginVerificationResult.Verified {
    val ide = buildSdkIde()
    return VerificationRunner().runPluginVerification(ide, idePlugin) as PluginVerificationResult.Verified
  }

  private fun buildSdkIde(): Ide =
    buildIdeWithBundledPlugins(includeKotlinStdLib = true) {
      dirs("com/intellij/platform/util/progress") {
        file("ExperimentalReporterHandle.class", ExperimentalReporterHandleDump.dump())
        file("ExperimentalProgressKt.class", ExperimentalProgressKtDump.dump())
      }
    }
}
