package com.jetbrains.pluginverifier.tasks.checkPlugin

import com.jetbrains.pluginverifier.options.CmdOpts
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
      //TODO use constant
      suppressInternalApiUsageWarnings = "internal-plugins"
    }

    val ideDescriptorParser = IdeDescriptorParser { _, _ -> emptyList() }

    val somePluginZipFile = createTempFile(suffix = ".zip")
    val someIde = createTempDirectory("idea-IU-117.963")

    val params = CheckPluginParamsBuilder(pluginRepository, pluginVerificationReportage, pluginDetailsCache, ideDescriptorParser)
      .build(cmdOpts, freeArgs = listOf(somePluginZipFile.absolutePathString(), someIde.absolutePathString()))

    assertEquals(InternalApiVerificationMode.IGNORE_IN_INTERNAL_PLUGINS, params.internalApiVerificationMode)
  }
}