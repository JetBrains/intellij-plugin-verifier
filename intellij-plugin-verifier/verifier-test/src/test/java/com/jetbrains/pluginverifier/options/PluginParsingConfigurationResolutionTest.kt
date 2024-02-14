package com.jetbrains.pluginverifier.options

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.pluginverifier.options.SubmissionType.EXISTING
import com.jetbrains.pluginverifier.options.SubmissionType.NEW
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import junit.framework.TestCase.assertTrue
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test

class PluginParsingConfigurationResolutionTest {
  private lateinit var configurationResolution: PluginParsingConfigurationResolution

  private val pluginId = "mock-plugin"
  private lateinit var plugin: MockIdePlugin

  @Before
  fun setUp() {
    configurationResolution = PluginParsingConfigurationResolution()
    plugin = MockIdePlugin(pluginId)

  }

  @Test
  fun `new plugin configuration is resolved`() {
    val config = PluginParsingConfiguration(pluginSubmissionType = NEW)
    val creationResultResolver = configurationResolution.resolveProblemLevelMapping(config) {
      levelRemappingFromClassPathJson()
    }

    val errors = listOf(
      ErroneousSinceBuild(PLUGIN_XML, IdeVersion.createIdeVersion("123"))
    )
    val warnings = listOf(SuspiciousUntilBuild("999"))
    val creationResult = creationResultResolver.resolve(plugin, errors + warnings)
    assertThat(creationResult, instanceOf(PluginCreationFail::class.java))
    val creationFail = creationResult as PluginCreationFail
    assertThat(creationFail.errorsAndWarnings.size, `is`(2))
  }

  @Test
  fun `existing plugin configuration is resolved`() {
    val config = PluginParsingConfiguration(pluginSubmissionType = EXISTING)
    val creationResultResolver = configurationResolution.resolveProblemLevelMapping(config) {
      levelRemappingFromClassPathJson()
    }

    assertThat(creationResultResolver, instanceOf(LevelRemappingPluginCreationResultResolver::class.java))
    val levelRemappingResolver = creationResultResolver as LevelRemappingPluginCreationResultResolver

    val problemsThatShouldBeIgnored = listOf(
      ForbiddenPluginIdPrefix(pluginId, "some.forbidden.plugin.id"),
      TemplateWordInPluginId(pluginId, "forbiddenTemplateWord"),
      TemplateWordInPluginName(pluginId, "forbiddenTemplateWord"),
    )
    val warnings = listOf(SuspiciousUntilBuild("999"))
    val creationResult = levelRemappingResolver.resolve(plugin, problemsThatShouldBeIgnored + warnings)
    assertTrue(creationResult is PluginCreationSuccess)
    val creationSuccess = creationResult as PluginCreationSuccess
    assertThat(creationSuccess.warnings.size, `is`(1))
  }

  @Test
  fun `existing plugin configuration with custom remapping definition`() {
    val config = PluginParsingConfiguration(pluginSubmissionType = EXISTING)
    val creationResultResolver = configurationResolution.resolveProblemLevelMapping(config) {
      object : PluginProblemLevelRemappingDefinitionManager {
        override fun initialize() = Definitions().apply {
          this["existing-plugin"] = mapOf(ErroneousSinceBuild::class to StandardLevel(PluginProblem.Level.WARNING))
        }
      }
    }

    assertThat(creationResultResolver, instanceOf(LevelRemappingPluginCreationResultResolver::class.java))
    val levelRemappingResolver = creationResultResolver as LevelRemappingPluginCreationResultResolver

    val problemsThatShouldBeRemapped = listOf(
      ForbiddenPluginIdPrefix(pluginId, "some.forbidden.plugin.id"),
    )
    val warnings = listOf(SuspiciousUntilBuild("999"))
    val creationResult = levelRemappingResolver.resolve(plugin, problemsThatShouldBeRemapped + warnings)
    assertTrue(creationResult is PluginCreationSuccess)
    val creationSuccess = creationResult as PluginCreationSuccess
    assertThat(creationSuccess.warnings.size, `is`(2))
  }
}