package com.jetbrains.pluginverifier.tests

import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.tests.mocks.buildCoreIde
import com.jetbrains.pluginverifier.tests.mocks.buildIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.descriptor
import com.jetbrains.pluginverifier.tests.mocks.ideaPlugin
import com.jetbrains.pluginverifier.usages.overrideOnly.dumpInterfaceWithDefaultMethodsAndLambdas
import org.junit.Assert.assertTrue
import org.junit.Test

class BytecodeTest  : BasePluginTest() {
  @Test
  fun `interface with default method using lambda invoking a default method in that interface`() {
    val pluginId = "pluginverifier"
    val descriptor = ideaPlugin(pluginId, "Mock Classpath")
    val plugin = pluginJarPath.buildIdePlugin {
      descriptor(descriptor)
      dir("mock") {
        file("Handler.class", dumpInterfaceWithDefaultMethodsAndLambdas().bytes)
      }
    }

    val ide = ideaPath.buildCoreIde()
    val verificationResult = VerificationRunner().runPluginVerification(ide, plugin)
    assertTrue(verificationResult is PluginVerificationResult.Verified)
    val verifiedResult = verificationResult as PluginVerificationResult.Verified
    assertEmpty("Compatibility problems", verifiedResult.compatibilityProblems)
  }
}