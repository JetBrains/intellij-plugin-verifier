package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.output.settings.dependencies.AllMissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.results.Result
import com.jetbrains.pluginverifier.results.Verdict
import com.jetbrains.pluginverifier.tests.mocks.MockPluginRepositoryAdapter
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

/**
 * @author Sergey Patrikeev
 */
class TestTeamCityResultPrinter {

  data class MockUpdateInfo(val id: String, val name: String, val version: String, val updateId: Int)

  private fun noConnectionPluginRepository() = object : MockPluginRepositoryAdapter() {
    override fun defaultAction(): Nothing = throw IOException("no connection")
  }

  private fun mockRepository(mockUpdateInfos: List<MockUpdateInfo>) = object : MockPluginRepositoryAdapter() {

    val updateInfos: List<UpdateInfo>
      get() = mockUpdateInfos.map { createMockUpdateInfo(it.id, it.name, it.version, it.updateId) }

    override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> = updateInfos

    override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? = updateInfos.find { it.pluginId == pluginId }

    override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<PluginInfo> = updateInfos.toList()
  }

  @Test
  fun `test newest suffix for updates with newest versions`() {
    val mockUpdateInfos = listOf(
        MockUpdateInfo("id", "name", "version", 1),
        MockUpdateInfo("id", "name", "version 2", 2)
    )
    val mockRepository = mockRepository(mockUpdateInfos)
    val updateInfos = mockRepository.updateInfos
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
    val output = getTeamCityOutput(mockPluginRepository, listOf(mockPluginRepository.createMockUpdateInfo("id", "name", "v", 1)))
    Assert.assertEquals("""##teamcity[testSuiteStarted name='id']
##teamcity[testStarted name='(v)']
##teamcity[testFinished name='(v)']
##teamcity[testSuiteFinished name='id']
""", output)
  }

  private fun getTeamCityOutput(pluginRepository: MockPluginRepositoryAdapter, pluginInfos: List<UpdateInfo>): String {
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