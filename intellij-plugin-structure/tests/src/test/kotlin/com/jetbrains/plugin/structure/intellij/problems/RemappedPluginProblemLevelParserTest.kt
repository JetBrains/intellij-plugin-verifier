package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.problems.PluginProblemsLoader.PluginProblemLevel.*
import junit.framework.TestCase.assertTrue
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class RemappedPluginProblemLevelParserTest {
  private val aForbiddenPluginIdPrefix = ForbiddenPluginIdPrefix::class
  private val aTemplateWordInPluginId = TemplateWordInPluginId::class
  private val aTemplateWordInPluginName = TemplateWordInPluginName::class

  private lateinit var remappingParser: RemappedPluginProblemLevelParser

  @Before
  fun setUp() {
    remappingParser = RemappedPluginProblemLevelParser()
  }

  @Test
  fun parse() {
    val definition = PluginProblemSet("test-definition", setOf(
      Ignored("ForbiddenPluginIdPrefix"),
      Error("TemplateWordInPluginId"),
      Warning("TemplateWordInPluginName")))
    val remapping = remappingParser.parse(definition)
    assertThat(remapping.size, `is`(3))

    assertThat(remapping[aForbiddenPluginIdPrefix], `is`(IgnoredLevel))
    assertThat(remapping[aTemplateWordInPluginId], `is`(StandardLevel(PluginProblem.Level.ERROR)))
    assertThat(remapping[aTemplateWordInPluginName], `is`(StandardLevel(PluginProblem.Level.WARNING)))
  }

  @Test
  fun `unsupported plugin level remapping is handled, but ignored`() {
    val definition = PluginProblemSet("test-definition", setOf(
      Ignored("UnsupportedPluginProblemThatShouldNotBeFound")))

    val remapping = remappingParser.parse(definition)
    assertTrue(remapping.isEmpty())
  }
}