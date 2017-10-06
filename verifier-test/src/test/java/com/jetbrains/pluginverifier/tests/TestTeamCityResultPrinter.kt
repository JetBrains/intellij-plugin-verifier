package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.output.settings.dependencies.AllMissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.tests.mocks.MockPluginRepositoryAdapter
import org.junit.Assert
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.PrintStream

/**
 * @author Sergey Patrikeev
 */
class TestTeamCityResultPrinter {

  private fun noConnectionPluginRepository() = object : MockPluginRepositoryAdapter() {
    override fun defaultAction(): Nothing = throw IOException("no connection")
  }

  private fun mockRepository(updateInfos: List<UpdateInfo>) = object : MockPluginRepositoryAdapter() {
    override fun getLastCompatibleUpdates(ideVersion: IdeVersion): List<UpdateInfo> = updateInfos

    override fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? = updateInfos.find { it.pluginId == pluginId }

    override fun getAllCompatibleUpdatesOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> = updateInfos.toList()
  }

  @Test
  fun `test newest suffix for updates with newest versions`() {
    val updateInfos = listOf(UpdateInfo("id", "name", "version", 1, ""), UpdateInfo("id", "name", "version 2", 2, ""))
    val mockRepository = mockRepository(updateInfos)
    val output = getTeamCityOutput(mockRepository, updateInfos)
    Assert.assertEquals("""##teamcity[testSuiteStarted name='id']
##teamcity[testStarted name='(version)']
##teamcity[testFinished name='(version)']
##teamcity[testStarted name='(version 2 - newest)']
##teamcity[testFinished name='(version 2 - newest)']
##teamcity[testSuiteFinished name='id']
""", output)
  }

  @Test
  fun `no repository connection lead to no -newest suffix`() {
    val mockPluginRepository = noConnectionPluginRepository()
    val output = getTeamCityOutput(mockPluginRepository, listOf(UpdateInfo("id", "name", "v", 1, "vendor")))
    Assert.assertEquals("""##teamcity[testSuiteStarted name='id']
##teamcity[testStarted name='(v)']
##teamcity[testFinished name='(v)']
##teamcity[testSuiteFinished name='id']
""", output)
  }

  private fun getTeamCityOutput(pluginRepository: PluginRepository, pluginInfos: List<PluginInfo>): String {
    val dependencyNode = DependencyNode("id", "version", emptyList())
    val output = ByteArrayOutputStream().use { bos ->
      PrintStream(bos, true, "utf-8").use { printStream ->
        val teamCityLog = TeamCityLog(printStream)
        val teamCityVPrinter = TeamCityResultPrinter(teamCityLog, TeamCityResultPrinter.GroupBy.BY_PLUGIN, pluginRepository, AllMissingDependencyIgnoring)
        teamCityVPrinter.printResults(
            pluginInfos.map {
              Result(
                  it,
                  IdeVersion.createIdeVersion("IU-145"),
                  Verdict.OK(DependenciesGraph(dependencyNode, listOf(dependencyNode), emptyList()))
              )
            }
        )
      }
      bos.toString("utf-8")
    }
    return output
  }
}