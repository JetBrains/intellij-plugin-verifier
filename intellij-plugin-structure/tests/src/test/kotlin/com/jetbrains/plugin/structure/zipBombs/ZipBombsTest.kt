package com.jetbrains.plugin.structure.zipBombs

import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.dotnet.ReSharperPluginManager
import com.jetbrains.plugin.structure.hub.HubPluginManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.mocks.BaseFileSystemAwareTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import com.jetbrains.plugin.structure.zipBombs.DecompressorSizeLimitTest.Companion.generateZipFileOfSizeAtLeast
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class ZipBombsTest(fileSystemType: FileSystemType) : BaseFileSystemAwareTest(fileSystemType) {

  private val properties = listOf(
    Settings.INTELLIJ_PLUGIN_SIZE_LIMIT,
    Settings.TEAM_CITY_PLUGIN_SIZE_LIMIT,
    Settings.RE_SHARPER_PLUGIN_SIZE_LIMIT,
    Settings.HUB_PLUGIN_SIZE_LIMIT
  )

  private val oldValues = mutableMapOf<Settings, String>()

  private val maxSize = 1000L

  @Before
  fun setUp() {
    for (property in properties) {
      oldValues[property] = property.get()
      property.set(maxSize.toString())
    }
  }

  @After
  fun tearDown() {
    for ((property, value) in oldValues) {
      property.set(value)
    }
  }

  @Test
  fun `all managers are protected against zip bombs`() {
    val testFolder = temporaryFolder.newFolder()
    val zipBomb = generateZipFileOfSizeAtLeast(testFolder.resolve("bomb.zip"), maxSize)
    val nuPkgBomb = Files.copy(zipBomb, testFolder.resolve("bomb.nupkg"))

    assertTrue(Files.size(nuPkgBomb) > maxSize)

    checkTooLargeProblem(IdePluginManager.createManager(), zipBomb)
    checkTooLargeProblem(IdePluginManager.createManager(temporaryFolder.newFolder()), zipBomb)
    checkTooLargeProblem(TeamcityPluginManager.createManager(), zipBomb)
    checkTooLargeProblem(TeamcityPluginManager.createManager(temporaryFolder.newFolder()), zipBomb)
    checkTooLargeProblem(HubPluginManager.createManager(), zipBomb)
    checkTooLargeProblem(HubPluginManager.createManager(temporaryFolder.newFolder()), zipBomb)
    checkTooLargeProblem(ReSharperPluginManager.createManager(), nuPkgBomb)
    checkTooLargeProblem(ReSharperPluginManager.createManager(temporaryFolder.newFolder()), nuPkgBomb)
  }

  private fun checkTooLargeProblem(manager: PluginManager<Plugin>, zipBomb: Path) {
    val expectedProblem = PluginFileSizeIsTooLarge(maxSize)
    val creationResult = manager.createPlugin(zipBomb)
    assertTrue("$creationResult", creationResult is PluginCreationFail)
    val errors = (creationResult as PluginCreationFail).errorsAndWarnings.filter { it.level == PluginProblem.Level.ERROR }
    assertEquals(listOf(expectedProblem), errors)
  }
}