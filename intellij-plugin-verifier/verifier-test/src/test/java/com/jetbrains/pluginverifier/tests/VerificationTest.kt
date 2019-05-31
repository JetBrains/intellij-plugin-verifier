package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.results.VerificationResult
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test

class VerificationTest {

  companion object {
    lateinit var verificationResult: VerificationResult

    @BeforeClass
    @JvmStatic
    fun beforeClass() {
      prepareTestSystemProperties()
      val ideaFile = findMockIdePath()
      val pluginFile = findMockPluginJarPath()
      verificationResult = VerificationRunner().runPluginVerification(ideaFile, pluginFile)
    }

    private fun prepareTestSystemProperties() {
      System.setProperty("plugin.verifier.test.private.interface.method.name", "privateInterfaceMethodTestName")
    }
  }

  @Test
  fun `check that missing dependency is detected`() {
    val missingDependencies = (verificationResult as VerificationResult.MissingDependencies).directMissingDependencies
    Assert.assertFalse(missingDependencies.isEmpty())
    println(missingDependencies)
    val expectedDep = setOf(MissingDependency(PluginDependencyImpl("MissingPlugin", true, false), "Plugin MissingPlugin is not found"))
    Assert.assertEquals(expectedDep, missingDependencies.toSet())
  }

  @Test
  fun `check that all problems are found`() {
    val expectedProblems = parseExpectedProblems().toSet()
    val actualProblems = verificationResult.compatibilityProblems.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription)
    }
    assertSetsEqual(expectedProblems, actualProblems)
  }

  @Test
  fun `check that all deprecated API usages are found`() {
    val expectedDeprecated = parseExpectedDeprecated().toSet()
    val actualDeprecated = verificationResult.deprecatedUsages.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription)
    }
    assertSetsEqual(expectedDeprecated, actualDeprecated)
  }

  @Test
  fun `check that all experimental API usages are found`() {
    val expectedExperimental = parseExpectedExperimental().toSet()
    val actualExperimental = verificationResult.experimentalApiUsages.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription)
    }
    assertSetsEqual(expectedExperimental, actualExperimental)
  }

  @Test
  fun `check that all override only violating method invocations are found`() {
    val expectedOverrideOnlyUsages = parseOverrideOnlyUsages().toSet()
    val actualOverrideOnly = verificationResult.overrideOnlyMethodUsages.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription)
    }
    assertSetsEqual(expectedOverrideOnlyUsages, actualOverrideOnly)
  }

  @Test
  fun `check that all internal API violating usages are found`() {
    val expectedInternalApiUsages = parseInternalApiUsages().toSet()
    val actualInternalUsages = verificationResult.internalApiUsages.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription)
    }
    assertSetsEqual(expectedInternalApiUsages, actualInternalUsages)
  }

  @Test
  fun `check that all non-extendable API violating usages are found`() {
    val expectedNonExtendableUsages = parseNonExtendable().toSet()
    val actualNonExtendableUsages = verificationResult.nonExtendableApiUsages.mapTo(hashSetOf()) {
      DescriptionHolder(it.shortDescription, it.fullDescription)
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
      Assert.fail("Sets are not equal")
    }
  }

}