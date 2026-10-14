package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.problems.InvalidPluginName
import com.jetbrains.plugin.structure.base.problems.MAX_NAME_LENGTH
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.TooLongPropertyValue
import com.jetbrains.plugin.structure.teamcity.problems.ForbiddenWordInPluginName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DESCRIPTOR = "teamcity-plugin.xml"

class TeamcityPluginDisplayNameValidationTest {
  @Test
  fun `valid display name has no problems`() {
    assertTrue(validateTeamcityPluginDisplayName(DESCRIPTOR, "Awesome Runner").isEmpty())
  }

  @Test
  fun `blank display name is not specified`() {
    listOf(null, "", " ", "\n\t").forEach { displayName ->
      val problems = validateTeamcityPluginDisplayName(DESCRIPTOR, displayName)
      assertEquals("Display name '$displayName'", 1, problems.size)
      val problem = problems.single()
      assertTrue("Display name '$displayName'", problem is PropertyNotSpecified)
      assertTrue("Display name '$displayName'", problem.message.contains("<display-name>"))
    }
  }

  @Test
  fun `forbidden words are reported`() {
    listOf("teamcity", "TeamCity", "plugin", "Plugin").forEach { word ->
      val displayName = "Awesome $word Runner"
      val problems = validateTeamcityPluginDisplayName(DESCRIPTOR, displayName)
      assertEquals("Display name '$displayName'", 1, problems.size)
      assertTrue("Display name '$displayName'", problems.single() is ForbiddenWordInPluginName)
    }
  }

  @Test
  fun `display name of maximum length has no problems`() {
    assertTrue(validateTeamcityPluginDisplayName(DESCRIPTOR, "a".repeat(MAX_NAME_LENGTH)).isEmpty())
  }

  @Test
  fun `too long display name is reported`() {
    val problems = validateTeamcityPluginDisplayName(DESCRIPTOR, "a".repeat(MAX_NAME_LENGTH + 1))
    assertEquals(1, problems.size)
    val problem = problems.single()
    assertTrue(problem is TooLongPropertyValue)
    assertTrue(problem.message.contains("'display-name'"))
  }

  @Test
  fun `display name with invalid characters is reported`() {
    listOf("!", "?", "@", "*", "é", "™").forEach { symbol ->
      val displayName = "Awesome${symbol}Runner"
      val problems = validateTeamcityPluginDisplayName(DESCRIPTOR, displayName)
      assertEquals("Display name '$displayName'", 1, problems.size)
      assertTrue("Display name '$displayName'", problems.single() is InvalidPluginName)
    }
  }
}
