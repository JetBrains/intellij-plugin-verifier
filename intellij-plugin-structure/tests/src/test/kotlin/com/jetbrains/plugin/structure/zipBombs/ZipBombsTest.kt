package com.jetbrains.plugin.structure.zipBombs

import com.jetbrains.plugin.structure.base.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.plugin.Plugin
import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginManager
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.dotnet.ReSharperPluginManager
import com.jetbrains.plugin.structure.hub.HubPluginManager
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.random.Random

class ZipBombsTest {

  @Rule
  @JvmField
  val tempFolder = TemporaryFolder()

  @Test
  fun `all managers are protected against zip bombs`() {
    val maxSize = 1000L
    val bombSize = maxSize * 100

    listOf(
        "intellij.structure.intellij.plugin.size.limit",
        "intellij.structure.team.city.plugin.size.limit",
        "intellij.structure.re.sharper.plugin.size.limit",
        "intellij.structure.hub.plugin.size.limit"
    ).forEach { System.setProperty(it, maxSize.toString()) }

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