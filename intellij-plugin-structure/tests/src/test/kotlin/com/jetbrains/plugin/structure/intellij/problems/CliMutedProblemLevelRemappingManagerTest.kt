package com.jetbrains.plugin.structure.intellij.problems

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CliMutedProblemLevelRemappingManagerTest {
  @Test
  fun `plugin level remapping is resolved`() {
    val remappingDefinition = CliMutedProblemLevelRemappingManager(listOf("ForbiddenPluginIdPrefix")).initialize()
    val cliMutedRemapping = remappingDefinition[CLI_MUTED] ?: emptyLevelRemapping(CLI_MUTED)
    val ignoredLevel = cliMutedRemapping[ForbiddenPluginIdPrefix::class]
    assertNotNull(ignoredLevel)
    assertThat(ignoredLevel, `is`(IgnoredLevel))
  }

  @Test
  fun `nonexistent plugin level remapping is not resolved`() {
    val remappingDefinition = CliMutedProblemLevelRemappingManager().initialize()
    val cliMutedRemapping = remappingDefinition[CLI_MUTED] ?: emptyLevelRemapping(CLI_MUTED)
    val nonexistentRemappedLevel = cliMutedRemapping[DuplicatedDependencyWarning::class]
    assertNull(nonexistentRemappedLevel)
  }

  @Test
  fun `unsupported CLI problem ID is not resolved`() {
    val remappingDefinition = CliMutedProblemLevelRemappingManager(listOf("UnsupportedProblemId")).initialize()
    val cliMutedRemapping = remappingDefinition[CLI_MUTED] ?: emptyLevelRemapping(CLI_MUTED)
    val nonexistentRemappedLevel = cliMutedRemapping[DuplicatedDependencyWarning::class]
    assertNull(nonexistentRemappedLevel)
  }

  @Test
  fun `retrieve single level remapping definition`() {
    val manager = CliMutedProblemLevelRemappingManager(listOf("ForbiddenPluginIdPrefix"))
    val levelRemappingDefinition = manager.asLevelRemappingDefinition()

    assertThat(levelRemappingDefinition.size, `is`(1))
    assertThat(levelRemappingDefinition[ForbiddenPluginIdPrefix::class], `is`(IgnoredLevel))
  }
}