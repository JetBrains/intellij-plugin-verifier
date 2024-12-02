package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.ide.IdeManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class VerificationTest {

  companion object {
    lateinit var verificationResult: PluginVerificationResult.Verified

    @BeforeClass
    @JvmStatic
    fun beforeClass() {
      prepareTestSystemProperties()
      val idePath = findMockIdePath()
      val pluginFile = findMockPluginJarPath()

      val ide = IdeManager.createManager().createIde(idePath)
      val plugin = (IdePluginManager.createManager().createPlugin(pluginFile) as PluginCreationSuccess).plugin
      verificationResult = VerificationRunner().runPluginVerification(ide, plugin) as PluginVerificationResult.Verified
    }

    private fun prepareTestSystemProperties() {
      System.setProperty("plugin.verifier.test.private.interface.method.name", "privateInterfaceMethodTestName")
    }
  }

  @Test
  fun `check that missing dependency is detected`() {
    val missingDependencies = verificationResult.dependenciesGraph.getDirectMissingDependencies()
    assertFalse(missingDependencies.isEmpty())
    println(missingDependencies)
    val expectedDep = MissingDependency(PluginDependencyImpl("MissingPlugin", true, false), "Dependency 'MissingPlugin' is not found in Bundled plugins of IU-211.500")
    assertThat(missingDependencies, hasItem(expectedDep))
  }

  @Test
  fun `check that module incompatibility is detected`() {
    val missingDependencies = verificationResult.dependenciesGraph.getDirectMissingDependencies()
    val expectedDep = MissingDependency(PluginDependencyImpl("com.intellij.modules.arbitrary.module", false, true), "The plugin is incompatible with module 'com.intellij.modules.arbitrary.module'")
    assertTrue("$expectedDep not in $missingDependencies", expectedDep in missingDependencies)
  }

  @Test
  fun `check that all problems are found`() {
    val expectedProblems = parseExpectedProblems().toSet()
    val actualProblems = verificationResult.compatibilityProblems.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription, DescriptionType.PROBLEM)
    }
    assertSetsEqual(expectedProblems, actualProblems)
  }

  @Test
  fun `check that all warnings are found`() {
    val expectedWarnings = parseExpectedWarnings().toSet()
    val actualWarnings = verificationResult.compatibilityWarnings.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription, DescriptionType.WARNING)
    }
    assertSetsEqual(expectedWarnings, actualWarnings)
  }

  @Test
  fun `check that all deprecated API usages are found`() {
    val expectedDeprecated = parseExpectedDeprecated().toSet()
    val actualDeprecated = verificationResult.deprecatedUsages.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription, DescriptionType.DEPRECATED)
    }
    assertSetsEqual(expectedDeprecated, actualDeprecated)
  }

  @Test
  fun `check that all experimental API usages are found`() {
    val expectedExperimental = parseExpectedExperimental().toSet()
    val actualExperimental = verificationResult.experimentalApiUsages.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription, DescriptionType.EXPERIMENTAL)
    }
    assertSetsEqual(expectedExperimental, actualExperimental)
  }

  @Test
  fun `check that all override only violating method invocations are found`() {
    val expectedOverrideOnlyUsages = parseOverrideOnlyUsages().toSet()
    val actualOverrideOnly = verificationResult.overrideOnlyMethodUsages.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription, DescriptionType.OVERRIDE_ONLY)
    }
    assertSetsEqual(expectedOverrideOnlyUsages, actualOverrideOnly)
  }

  @Test
  fun `check that all internal API violating usages are found`() {
    val expectedInternalApiUsages = parseInternalApiUsages().toSet()
    val actualInternalUsages = verificationResult.internalApiUsages.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription, DescriptionType.INTERNAL)
    }
    assertSetsEqual(expectedInternalApiUsages, actualInternalUsages)
  }

  @Test
  fun `check that all non-extendable API violating usages are found`() {
    val expectedNonExtendableUsages = parseNonExtendable().toSet()
    val actualNonExtendableUsages = verificationResult.nonExtendableApiUsages.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription, DescriptionType.NON_EXTENDABLE)
    }
    assertSetsEqual(expectedNonExtendableUsages, actualNonExtendableUsages)
  }

  private fun assertSetsEqual(expected: Set<DescriptionHolder>, actual: Set<DescriptionHolder>) {
    val allRedundant = actual - expected
    val allMissing = expected - actual

    fun Set<DescriptionHolder>.findSimilar(descriptionHolder: DescriptionHolder): DescriptionHolder? {
      return find { it.fullDescription == descriptionHolder.fullDescription }
        ?: find { it.shortDescription == descriptionHolder.shortDescription }
    }

    val separator = "-".repeat(10)
    if (allMissing.isNotEmpty()) {
      System.err.println("The following must be reported but are not:\n")
      for (missing in allMissing) {
        System.err.println(separator)
        System.err.println(missing)

        val similar = actual.findSimilar(missing)
        if (similar != null) {
          System.err.println("Similar actual:")
          System.err.println(similar)
        }
        System.err.println(separator)
      }
    }

    if (allRedundant.isNotEmpty()) {
      System.err.println("Unexpectedly reported:\n")
      for (redundant in allRedundant) {
        System.err.println(separator)
        System.err.println(redundant)

        val similar = expected.findSimilar(redundant)
        if (similar != null) {
          System.err.println("Similar expected:")
          System.err.println(similar)
        }
        System.err.println(separator)
      }
    }

    if (allRedundant.isNotEmpty() || allMissing.isNotEmpty()) {
      val message = buildString {
        append("Sets are not equal.")
        append(if (allRedundant.isNotEmpty()) " Unexpectingly reported ${allRedundant.size} failed assertions" else "")
        append(if (allMissing.isNotEmpty()) " Expected ${allMissing.size} failed assertions" else "")
      }
      Assert.fail(message)
    }
  }

}