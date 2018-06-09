package com.jetbrains.pluginverifier.tests

import com.jetbrains.pluginverifier.VerificationTarget
import com.jetbrains.pluginverifier.dependencies.DependenciesGraph
import com.jetbrains.pluginverifier.dependencies.DependencyNode
import com.jetbrains.pluginverifier.output.settings.dependencies.AllMissingDependencyIgnoring
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.results.VerificationResult
import com.jetbrains.pluginverifier.tests.mocks.MockPluginRepositoryAdapter
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class TestTeamCityResultPrinter {

  data class MockUpdateInfo(val id: String, val name: String, val version: String, val updateId: Int)

  private fun noConnectionPluginRepository() = object : MockPluginRepositoryAdapter() {
    override fun defaultAction(): Nothing = throw IOException("no connection")
  }

  private fun mockRepository(mockUpdateInfos: List<MockUpdateInfo>) = object : MockPluginRepositoryAdapter() {

    val pluginInfos: List<PluginInfo>
      get() = mockUpdateInfos.map { createMockPluginInfo(it.id, it.version) }

    override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> = pluginInfos

    override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): PluginInfo? = pluginInfos.find { it.pluginId == pluginId }

    override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<PluginInfo> = pluginInfos.toList()
  }

  @Test
  fun `test newest suffix for updates with newest versions`() {
    val mockUpdateInfos = listOf(
        MockUpdateInfo("id", "name", "version", 1),
        MockUpdateInfo("id", "name", "version 2", 2)
    )
    val mockRepository = mockRepository(mockUpdateInfos)
    val updateInfos = mockRepository.pluginInfos
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
    val output = getTeamCityOutput(mockPluginRepository, listOf(mockPluginRepository.createMockPluginInfo("id", "v")))
    Assert.assertEquals("""##teamcity[testSuiteStarted name='id']
##teamcity[testStarted name='(v)']
##teamcity[testFinished name='(v)']
##teamcity[testSuiteFinished name='id']
""", output)
  }

  private fun getTeamCityOutput(pluginRepository: MockPluginRepositoryAdapter, pluginInfos: List<PluginInfo>): String {
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
              VerificationResult.OK().apply {
                plugin = it
                verificationTarget = VerificationTarget.Ide(IdeVersion.createIdeVersion("IU-145"))
                dependenciesGraph = DependenciesGraph(dependencyNode, listOf(dependencyNode), emptyList())
              }
            }
        )
      stringWriter.toString()
    }
  }
}