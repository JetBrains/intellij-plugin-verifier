package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.plugin.structure.intellij.plugin.caches.SimplePluginArchiveManager
import com.jetbrains.pluginverifier.options.CmdOpts
import com.jetbrains.pluginverifier.options.OptionsParser
import com.jetbrains.pluginverifier.options.SubmissionType
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.reporting.PluginVerificationReportage
import com.jetbrains.pluginverifier.repository.PluginRepository
import com.jetbrains.pluginverifier.tests.mocks.MockPluginDetailsCache
import com.jetbrains.pluginverifier.tests.mocks.MockPluginRepositoryAdapter
import com.jetbrains.pluginverifier.tests.mocks.MockPluginVerificationReportage
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.io.path.createTempFile

class CheckPluginParamsBuilderTest {
  private lateinit var pluginRepository: PluginRepository

  private lateinit var pluginVerificationReportage: PluginVerificationReportage

  private lateinit var pluginDetailsCache: PluginDetailsCache

  @Before
  fun setUp() {
    pluginRepository = MockPluginRepositoryAdapter()
    pluginVerificationReportage = MockPluginVerificationReportage()
    pluginDetailsCache = MockPluginDetailsCache()
  }

  @Test
  fun `internal API switch is parsed`() {
    val cmdOpts = CmdOpts().apply {
      suppressInternalApiUsageWarnings = JETBRAINS_PLUGINS_API_USAGE_MODE
    }

    val ideDescriptorParser = IdeDescriptorParser { _, _ -> emptyList() }

    val somePluginZipFile = createTempFile(suffix = ".zip")
    val someIde = createTempDirectory("idea-IU-117.963")

    val params = CheckPluginParamsBuilder(
      pluginRepository,
      pluginVerificationReportage,
      pluginDetailsCache,
      SimplePluginArchiveManager(),
      ideDescriptorParser
    )
      .build(cmdOpts, freeArgs = listOf(somePluginZipFile.absolutePathString(), someIde.absolutePathString()))

    assertEquals(InternalApiVerificationMode.IGNORE_IN_JETBRAINS_PLUGINS, params.internalApiVerificationMode)
  }

  @Test
  fun `existing plugin verification is parsed`() {
    val cmdOpts = CmdOpts().apply {
      submissionType = "existing"
    }

    val ideDescriptorParser = IdeDescriptorParser { _, _ -> emptyList() }

    val somePluginZipFile = createTempFile(suffix = ".zip")
    val someIde = createTempDirectory("idea-IU-117.963")

    val params = CheckPluginParamsBuilder(
      pluginRepository,
      pluginVerificationReportage,
      pluginDetailsCache,
      SimplePluginArchiveManager(),
      ideDescriptorParser
    )
      .build(cmdOpts, freeArgs = listOf(somePluginZipFile.absolutePathString(), someIde.absolutePathString()))

    assertEquals(SubmissionType.EXISTING, params.pluginSubmissionType)
  }

  @Test
  fun `CLI-ignored plugin problems is parsed`() {
    val cmdOpts = CmdOpts().apply {
      mutedPluginProblems = arrayOf("ForbiddenPluginIdPrefix", "TemplateWordInPluginId")
    }

    val pluginParsingConfiguration = OptionsParser.createPluginParsingConfiguration(cmdOpts)
    assertEquals(listOf("ForbiddenPluginIdPrefix", "TemplateWordInPluginId"), pluginParsingConfiguration.ignoredPluginProblems)
  }
}