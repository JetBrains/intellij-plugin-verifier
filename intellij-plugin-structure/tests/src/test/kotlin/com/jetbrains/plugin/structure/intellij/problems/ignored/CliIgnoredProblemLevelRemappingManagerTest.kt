package com.jetbrains.plugin.structure.intellij.problems.ignored

import com.jetbrains.plugin.structure.intellij.problems.DuplicatedDependencyWarning
import com.jetbrains.plugin.structure.intellij.problems.ForbiddenPluginIdPrefix
import com.jetbrains.plugin.structure.intellij.problems.IgnoredLevel
import com.jetbrains.plugin.structure.intellij.problems.emptyLevelRemapping
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CliIgnoredProblemLevelRemappingManagerTest {
  @Test
  fun `plugin level remapping is resolved`() {
    val remappingDefinition = CliIgnoredProblemLevelRemappingManager(listOf("ForbiddenPluginIdPrefix")).initialize()
    val cliIgnoredRemapping = remappingDefinition[CLI_IGNORED] ?: emptyLevelRemapping(CLI_IGNORED)
    val ignoredLevel = cliIgnoredRemapping[ForbiddenPluginIdPrefix::class]
    assertNotNull(ignoredLevel)
    assertThat(ignoredLevel, `is`(IgnoredLevel))
  }

  @Test
  fun `nonexistent plugin level remapping is not resolved`() {
    val remappingDefinition = CliIgnoredProblemLevelRemappingManager().initialize()
    val cliIgnoredRemapping = remappingDefinition[CLI_IGNORED] ?: emptyLevelRemapping(CLI_IGNORED)
    val nonexistentRemappedLevel = cliIgnoredRemapping[DuplicatedDependencyWarning::class]
    assertNull(nonexistentRemappedLevel)
  }

  @Test
  fun `unsupported CLI problem ID is not resolved`() {
    val remappingDefinition = CliIgnoredProblemLevelRemappingManager(listOf("UnsupportedProblemId")).initialize()
    val cliIgnoredRemapping = remappingDefinition[CLI_IGNORED] ?: emptyLevelRemapping(CLI_IGNORED)
    val nonexistentRemappedLevel = cliIgnoredRemapping[DuplicatedDependencyWarning::class]
    assertNull(nonexistentRemappedLevel)
  }

  @Test
  fun `retrieve single level remapping definition`() {
    val manager = CliIgnoredProblemLevelRemappingManager(listOf("ForbiddenPluginIdPrefix"))
    val levelRemappingDefinition = manager.asLevelRemappingDefinition()

    assertThat(levelRemappingDefinition.size, `is`(1))
    assertThat(levelRemappingDefinition[ForbiddenPluginIdPrefix::class], `is`(IgnoredLevel))
  }
}