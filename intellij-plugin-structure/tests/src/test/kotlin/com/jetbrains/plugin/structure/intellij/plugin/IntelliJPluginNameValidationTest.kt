package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.problems.InvalidPluginName
import com.jetbrains.plugin.structure.base.problems.MAX_NAME_LENGTH
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.PropertyWithDefaultValue
import com.jetbrains.plugin.structure.base.problems.TooLongPropertyValue
import com.jetbrains.plugin.structure.intellij.problems.TemplateWordInPluginName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DESCRIPTOR_PATH = "plugin.xml"

class IntelliJPluginNameValidationTest {
  @Test
  fun `valid name has no problems`() {
    assertTrue(validateIntelliJPluginName(DESCRIPTOR_PATH, "Awesome Tool").isEmpty())
  }

  @Test
  fun `whitespace around name is ignored`() {
    assertTrue(validateIntelliJPluginName(DESCRIPTOR_PATH, "\n    Awesome Tool\n  ").isEmpty())
    assertTrue(validateIntelliJPluginName(DESCRIPTOR_PATH, " " + "a".repeat(MAX_NAME_LENGTH) + " ").isEmpty())
  }

  @Test
  fun `blank name is not specified`() {
    listOf(null, "", " ", "\n\t").forEach { name ->
      val problems = validateIntelliJPluginName(DESCRIPTOR_PATH, name)
      assertEquals("Name '$name'", 1, problems.size)
      assertTrue("Name '$name'", problems.single() is PropertyNotSpecified)
    }
  }

  @Test
  fun `default template names are reported`() {
    val templateNames = listOf("Plugin display name here", "My Framework Support", "Template", "Demo")
    templateNames.flatMap { listOf(it, it.lowercase()) }.forEach { name ->
      val problems = validateIntelliJPluginName(DESCRIPTOR_PATH, name)
      assertEquals("Template name '$name'", 1, problems.size)
      val problem = problems.single()
      assertTrue("Template name '$name'", problem is PropertyWithDefaultValue)
      assertTrue("Template name '$name'", problem.message.contains("<name>"))
    }
  }

  @Test
  fun `restricted words are reported`() {
    val restrictedWords = listOf(
      "plugin", "JetBrains", "IDEA", "PyCharm", "CLion", "AppCode", "DataGrip", "Fleet", "GoLand", "PhpStorm",
      "WebStorm", "Rider", "ReSharper", "TeamCity", "YouTrack", "RubyMine", "IntelliJ"
    )
    restrictedWords.forEach { word ->
      val problems = validateIntelliJPluginName(DESCRIPTOR_PATH, "Awesome $word Tool")
      assertEquals("Restricted word '$word'", 1, problems.size)
      val problem = problems.single()
      assertTrue("Restricted word '$word'", problem is TemplateWordInPluginName)
      assertTrue("Restricted word '$word'", problem.message.contains("'$word'"))
    }
  }

  @Test
  fun `name of maximum length has no problems`() {
    assertTrue(validateIntelliJPluginName(DESCRIPTOR_PATH, "a".repeat(MAX_NAME_LENGTH)).isEmpty())
  }

  @Test
  fun `too long name is reported`() {
    val problems = validateIntelliJPluginName(DESCRIPTOR_PATH, "a".repeat(MAX_NAME_LENGTH + 1))
    assertEquals(1, problems.size)
    assertTrue(problems.single() is TooLongPropertyValue)
  }

  @Test
  fun `name with invalid characters is reported`() {
    listOf("!", "?", "@", "*", "é", "™").forEach { symbol ->
      val name = "Awesome${symbol}Tool"
      val problems = validateIntelliJPluginName(DESCRIPTOR_PATH, name)
      assertEquals("Name '$name'", 1, problems.size)
      assertTrue("Name '$name'", problems.single() is InvalidPluginName)
    }
  }
}
