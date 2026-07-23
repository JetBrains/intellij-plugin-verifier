package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.results.problems.CompatibilityProblem
import com.jetbrains.pluginverifier.tests.bytecode.DirectCallerDump
import com.jetbrains.pluginverifier.tests.bytecode.InlinedCallerDump
import com.jetbrains.pluginverifier.tests.bytecode.OwnInlineCallerDump
import com.jetbrains.pluginverifier.tests.bytecode.PluginUtilsKtDump
import com.jetbrains.pluginverifier.tests.bytecode.RawProgressKtDump
import com.jetbrains.pluginverifier.tests.bytecode.RawProgressReporterHandleDump
import com.jetbrains.pluginverifier.tests.bytecode.SuspendLambdaCallerDump
import com.jetbrains.pluginverifier.tests.mocks.IdeaPluginSpec
import com.jetbrains.pluginverifier.usages.internal.InternalApiUsage
import com.jetbrains.pluginverifier.usages.internal.InternalMethodUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Reproduces [MP-7133](https://youtrack.jetbrains.com/issue/MP-7133): the Kotlin compiler copies
 * the body of a public SDK `inline fun` — including its references to `@ApiStatus.Internal` API —
 * into the plugin's class files. Such usages must not be attributed to the plugin, whose author
 * only ever called public API. The mimicked sources are quoted in each dump's KDoc, see [InlinedCallerDump].
 */
class KotlinInlineFunctionInternalApiUsageTest : BaseBytecodeTest() {

  private val thirdPartyPluginSpec = IdeaPluginSpec("com.example.progress", "Third Party Inc.")

  @Test
  fun `internal API referenced only from the inlined body of a public SDK inline function is not reported`() {
    val idePlugin = buildIdePlugin(thirdPartyPluginSpec) {
      dirs("com/example/plugin") {
        file("MyAction.class", InlinedCallerDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertEquals(emptySet<CompatibilityProblem>(), verificationResult.compatibilityProblems)
    assertEquals(
      "Internal API usages coming from an inlined body of a public SDK inline function " +
        "must not be attributed to the plugin",
      emptySet<InternalApiUsage>(),
      verificationResult.internalApiUsages
    )
  }

  @Test
  fun `internal API inlined into the invokeSuspend method of a suspend lambda is not reported`() {
    val idePlugin = buildIdePlugin(thirdPartyPluginSpec) {
      dirs("com/example/plugin") {
        file("MyAction\$run\$1.class", SuspendLambdaCallerDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertEquals(emptySet<InternalApiUsage>(), verificationResult.internalApiUsages)
  }

  @Test
  fun `internal API usage suppression works when the SMAP survives only in the SourceDebugExtension annotation`() {
    val idePlugin = buildIdePlugin(thirdPartyPluginSpec) {
      dirs("com/example/plugin") {
        file("MyAction.class", InlinedCallerDump.dump(smapInSourceAttribute = false))
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertEquals(
      "The SourceDebugExtension attribute was stripped, but the SMAP is still available " +
        "in the kotlin.jvm.internal.SourceDebugExtension annotation",
      emptySet<InternalApiUsage>(),
      verificationResult.internalApiUsages
    )
  }

  @Test
  fun `internal API inlined from the plugin's own inline function is still reported`() {
    val idePlugin = buildIdePlugin(thirdPartyPluginSpec) {
      dirs("com/example/plugin") {
        file("OwnCaller.class", OwnInlineCallerDump.dump())
        file("UtilsKt.class", PluginUtilsKtDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertTrue(
      "Internal API usage inlined from the plugin's own inline function is the plugin author's " +
        "own code and must still be reported",
      verificationResult.internalApiUsages
        .filterIsInstance<InternalMethodUsage>()
        .any { "internalCurrentStepAsRaw" in it.fullDescription }
    )
  }

  @Test
  fun `internal API invoked directly by plugin code is reported`() {
    val idePlugin = buildIdePlugin(thirdPartyPluginSpec) {
      dirs("com/example/plugin") {
        file("DirectUsage.class", DirectCallerDump.dump())
      }
    }

    val verificationResult = runVerification(idePlugin)

    assertEquals(emptySet<CompatibilityProblem>(), verificationResult.compatibilityProblems)
    assertTrue(
      "Direct invocation of an internal API method must be reported",
      verificationResult.internalApiUsages
        .filterIsInstance<InternalMethodUsage>()
        .any { "internalCurrentStepAsRaw" in it.fullDescription }
    )
  }

  private fun runVerification(idePlugin: IdePlugin): PluginVerificationResult.Verified {
    val ide = buildSdkIde()
    return VerificationRunner().runPluginVerification(ide, idePlugin) as PluginVerificationResult.Verified
  }

  private fun buildSdkIde(): Ide =
    buildIdeWithBundledPlugins(includeKotlinStdLib = true) {
      dirs("com/intellij/platform/util/progress") {
        file("RawProgressReporterHandle.class", RawProgressReporterHandleDump.dump())
        file("RawProgressKt.class", RawProgressKtDump.dump())
      }
    }
}
