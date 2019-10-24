package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.PluginVerificationResult
import com.jetbrains.pluginverifier.PluginVerificationTarget
import com.jetbrains.pluginverifier.jdk.JdkVersion
import com.jetbrains.pluginverifier.output.teamcity.TeamCityLog
import com.jetbrains.pluginverifier.output.teamcity.TeamCityResultPrinter
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.tests.mocks.MockPluginRepositoryAdapter
import com.jetbrains.pluginverifier.tests.mocks.createMockPluginInfo
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class TeamCityResultPrinterTest {

  private fun noConnectionPluginRepository() = object : MockPluginRepositoryAdapter() {
    override fun defaultAction(): Nothing = throw IOException("no connection")
  }

  @Test
  fun `test newest suffix for updates with newest versions`() {
    val first = createMockPluginInfo("id", "version")
    val second = createMockPluginInfo("id", "version 2")

    val mockRepository = object : MockPluginRepositoryAdapter() {
      val pluginInfos: List<PluginInfo>
        get() = listOf(first, second)

      override fun getLastCompatiblePlugins(ideVersion: IdeVersion): List<PluginInfo> = pluginInfos

      override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String): PluginInfo? =
        second.takeIf { pluginId == it.pluginId }

      override fun getAllCompatibleVersionsOfPlugin(ideVersion: IdeVersion, pluginId: String): List<PluginInfo> =
        pluginInfos.takeIf { pluginId == "id" }.orEmpty()
    }

    val updateInfos = mockRepository.pluginInfos
    val output = getTeamCityOutput(mockRepository, updateInfos)
    Assert.assertEquals(
      """##teamcity[testSuiteStarted name='id']
##teamcity[testStarted name='(version)']
##teamcity[testFinished name='(version)']
##teamcity[testStarted name='(version 2 - newest)']
##teamcity[testFinished name='(version 2 - newest)']
##teamcity[testSuiteFinished name='id']
""", output
    )
  }

  @Test
  fun `no repository connection lead to no -newest suffix`() {
    val mockPluginRepository = noConnectionPluginRepository()
    val output = getTeamCityOutput(mockPluginRepository, listOf(createMockPluginInfo("id", "v")))
    Assert.assertEquals(
      """##teamcity[testSuiteStarted name='id']
##teamcity[testStarted name='(v)']
##teamcity[testFinished name='(v)']
##teamcity[testSuiteFinished name='id']
""", output
    )
  }

  private fun getTeamCityOutput(pluginRepository: MockPluginRepositoryAdapter, pluginInfos: List<PluginInfo>): String {
    return StringWriter().use { stringWriter ->
      val tcLog = TeamCityLog(PrintWriter(stringWriter))
      val tcPrinter = TeamCityResultPrinter(
        tcLog,
        TeamCityResultPrinter.GroupBy.BY_PLUGIN,
        pluginRepository
      )
      val verificationTarget = PluginVerificationTarget.IDE(IdeVersion.createIdeVersion("IU-145"), JdkVersion("1.8", null))
      tcPrinter.printResults(
        pluginInfos.map {
          PluginVerificationResult.NotFound(
            it,
            verificationTarget,
            "Repository is off"
          )
        }
      )
      stringWriter.toString()
    }
  }
}