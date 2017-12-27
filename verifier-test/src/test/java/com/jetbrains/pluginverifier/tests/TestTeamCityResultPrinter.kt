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
import com.jetbrains.pluginverifier.tests.mocks.createMockUpdateInfo
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

/**
 * @author Sergey Patrikeev
 */
class TestTeamCityResultPrinter {

  private fun noConnectionPluginRepository() = object : MockPluginRepositoryAdapter() {
    override fun defaultAction(): Nothing = throw IOException("no connection")
  }

  private fun mockRepository(updateInfos: List<UpdateInfo>) = object : MockPluginRepositoryAdapter() {
    override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<UpdateInfo> = updateInfos

    override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? = updateInfos.find { it.pluginId == pluginId }

    override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<UpdateInfo> = updateInfos.toList()
  }

  @Test
  fun `test newest suffix for updates with newest versions`() {
    val updateInfos = listOf(
        createMockUpdateInfo("id", "name", "version", 1),
        createMockUpdateInfo("id", "name", "version 2", 2)
    )
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
    val output = getTeamCityOutput(mockPluginRepository, listOf(createMockUpdateInfo("id", "name", "v", 1)))
    Assert.assertEquals("""##teamcity[testSuiteStarted name='id']
##teamcity[testStarted name='(v)']
##teamcity[testFinished name='(v)']
##teamcity[testSuiteFinished name='id']
""", output)
  }

  private fun getTeamCityOutput(pluginRepository: PluginRepository, pluginInfos: List<PluginInfo>): String {
    val dependencyNode = DependencyNode("id", "version", emptyList())
    return StringWriter().use { stringWriter ->
      val tcLog = TeamCityLog(PrintWriter(stringWriter))
      val tcPrinter = TeamCityResultPrinter(
          tcLog,
          TeamCityResultPrinter.GroupBy.BY_PLUGIN,
          pluginRepository,
          AllMissingDependencyIgnoring
      )
      tcPrinter.printResults(
            pluginInfos.map {
              Result(
                  it,
                  IdeVersion.createIdeVersion("IU-145"),
                  Verdict.OK(DependenciesGraph(dependencyNode, listOf(dependencyNode), emptyList()), emptySet()),
                  emptySet()
              )
            }
        )
      stringWriter.toString()
    }
  }
}