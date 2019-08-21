package com.jetbrains.plugin.structure.zipBombs

import com.jetbrains.plugin.structure.base.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.plugin.*
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.dotnet.ReSharperPluginManager
import com.jetbrains.plugin.structure.hub.HubPluginManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random

class ZipBombsTest {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  private val properties = listOf(
      Settings.INTELLIJ_PLUGIN_SIZE_LIMIT,
      Settings.TEAM_CITY_PLUGIN_SIZE_LIMIT,
      Settings.RE_SHARPER_PLUGIN_SIZE_LIMIT,
      Settings.HUB_PLUGIN_SIZE_LIMIT
  )

  private val oldValues = mutableMapOf<Settings, String>()

  private val maxSize = 1000L
  private val bombSize = maxSize * 100

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
    val random = Random(42)
    val zipBomb = buildZipFile(tempFolder.newFile("bomb.zip")) {
      file("bomb.bin", random.nextBytes(bombSize.toInt()))
    }

    val nuPkgBomb = zipBomb.copyTo(zipBomb.resolveSibling("bomb.nupkg"))

    assertTrue(zipBomb.length() > maxSize)
    assertTrue(nuPkgBomb.length() > maxSize)

    val expectedProblem = PluginFileSizeIsTooLarge(maxSize)
    checkProblems(IdePluginManager.createManager(), zipBomb, expectedProblem)
    checkProblems(TeamcityPluginManager.createManager(), zipBomb, expectedProblem)
    checkProblems(HubPluginManager.createManager(), zipBomb, expectedProblem)
    checkProblems(ReSharperPluginManager, nuPkgBomb, expectedProblem)
  }

  private fun checkProblems(manager: PluginManager<Plugin>, zipBomb: File, expectedProblem: PluginProblem) {
    val creationResult = manager.createPlugin(zipBomb)
    assertTrue("$creationResult", creationResult is PluginCreationFail)
    val errors = (creationResult as PluginCreationFail).errorsAndWarnings.filter { it.level == PluginProblem.Level.ERROR }
    assertEquals(listOf(expectedProblem), errors)
  }
}