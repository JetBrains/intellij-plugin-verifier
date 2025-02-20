package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.tests.BasePluginTest
import com.jetbrains.pluginverifier.tests.VerificationRunner
import com.jetbrains.pluginverifier.tests.mocks.buildCoreIde
import com.jetbrains.pluginverifier.tests.mocks.buildIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.descriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BundledModuleDependencyTest : BasePluginTest() {
  lateinit var plugin: IdePlugin

  @Before
  fun setUp() {
    val pluginId = "pluginverifier"
    val descriptor = ideaPlugin(pluginId, "Mock Classpath") +
      """
        <dependencies>
          <plugin id="com.intellij.platform.ide.provisioner" />
        </dependencies>        
      """.trimIndent()
    plugin = pluginJarPath.buildIdePlugin {
      descriptor(descriptor)
    }
  }

  @Test
  fun `plugin declares plugin dependency on a bundled plugin via v2 semantics`() {
    val ide = ideaPath.buildCoreIde(additionalPluginXmlContent = """
      <module value="com.intellij.platform.ide.provisioner"/>
    """.trimIndent())
    val verificationResult = VerificationRunner().runPluginVerification(ide, plugin)
    assertTrue(verificationResult is PluginVerificationResult.Verified)
    val verifiedResult = verificationResult as PluginVerificationResult.Verified
    assertEmpty("Compatibility problems", verifiedResult.compatibilityProblems)
    assertEmpty("Direct Missing Mandatory Dependencies", verifiedResult.directMissingMandatoryDependencies)
  }

  @Test
  fun `plugin declares plugin dependency on a bundled plugin via v2 semantics but that plugin is not in the IDE`() {
    val ide = ideaPath.buildCoreIde()
    val verificationResult = VerificationRunner().runPluginVerification(ide, plugin)
    assertTrue(verificationResult is PluginVerificationResult.Verified)
    val verifiedResult = verificationResult as PluginVerificationResult.Verified
    assertEmpty("Compatibility problems", verifiedResult.compatibilityProblems)
    with(verifiedResult.directMissingMandatoryDependencies) {
      assertEquals(1, size)
      assertEquals(first().dependency.id, "com.intellij.platform.ide.provisioner")
    }
  }
}
