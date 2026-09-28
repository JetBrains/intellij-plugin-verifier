package com.jetbrains.plugin.structure.mocks.validation

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DependentPropertyProblemTest {

  @Test
  fun `dependent property problem has ERROR level by default`() {
    val problem = DependentPropertyProblem("Test message")
    assertEquals(PluginProblem.Level.ERROR, problem.level)
    assertEquals("Test message", problem.message)
    assertEquals("Test message", problem.toString())
  }

  @Test
  fun `dependent property problem equals and hashCode`() {
    val problem1 = DependentPropertyProblem("Test message")
    val problem2 = DependentPropertyProblem("Test message")
    val problem3 = DependentPropertyProblem("Different message")

    assertEquals(problem1, problem2)
    assertEquals(problem1.hashCode(), problem2.hashCode())
    assertNotEquals(problem1, problem3)
  }

  @Test
  fun `module count mismatch problem diagnostic message`() {
    val problem = ModuleCountMismatchProblem(descriptorCount = 1, contentModuleCount = 2)
    assertEquals(PluginProblem.Level.ERROR, problem.level)
    assertEquals("The number of 'modulesDescriptors' (1) does not match the number of 'contentModules' (2).", problem.message)
  }

  @Test
  fun `module identifier mismatch problem diagnostic message`() {
    val problem = ModuleIdentifierMismatchProblem(
      descriptorIdentifiers = listOf("foo"),
      contentModuleIdentifiers = listOf("bar")
    )
    assertEquals(PluginProblem.Level.ERROR, problem.level)
    assertEquals("The module identifiers in 'modulesDescriptors' ([foo]) do not match the identifiers in 'contentModules' ([bar]).", problem.message)
  }

  @Test
  fun `duplicate module name problem diagnostic message`() {
    val problem = DuplicateModuleNameProblem(
      duplicateNames = listOf("foo"),
      propertyName = "modulesDescriptors"
    )
    assertEquals(PluginProblem.Level.ERROR, problem.level)
    assertEquals("Duplicate module names found in 'modulesDescriptors' ([foo]).", problem.message)
  }
}
