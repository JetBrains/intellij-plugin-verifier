package com.jetbrains.plugin.structure.base.problems

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DESCRIPTOR = "descriptor.json"

class PluginNameValidationTest {
  @Test
  fun `valid name has no problems`() {
    assertTrue(validate("Awesome Tool").isEmpty())
  }

  @Test
  fun `name with all allowed symbols has no problems`() {
    assertTrue(validate("Tool 1 .,+_-/:()#'&[]|").isEmpty())
  }

  @Test
  fun `name of maximum length has no problems`() {
    assertTrue(validate("a".repeat(MAX_NAME_LENGTH)).isEmpty())
  }

  @Test
  fun `too long name is reported`() {
    val problems = validate("a".repeat(MAX_NAME_LENGTH + 1), propertyName = "title")
    assertEquals(1, problems.size)
    val problem = problems.single()
    assertTrue(problem is TooLongPropertyValue)
    assertTrue(problem.message.contains("'title'"))
  }

  @Test
  fun `name with invalid characters is reported`() {
    listOf("!", "?", "@", "*", "\"", "é", "™", "\t", "\n").forEach { symbol ->
      val name = "Awesome${symbol}Tool"
      val problems = validate(name)
      assertEquals("Name '$name'", 1, problems.size)
      assertTrue("Name '$name'", problems.single() is InvalidPluginName)
    }
  }

  @Test
  fun `too long name with invalid characters reports both problems`() {
    val problems = validate("!".repeat(MAX_NAME_LENGTH + 1))
    assertEquals(2, problems.size)
    assertTrue(problems[0] is TooLongPropertyValue)
    assertTrue(problems[1] is InvalidPluginName)
  }

  private fun validate(name: String, propertyName: String = "name"): List<PluginProblem> =
    mutableListOf<PluginProblem>().also { validatePluginName(DESCRIPTOR, propertyName, name, it) }
}
