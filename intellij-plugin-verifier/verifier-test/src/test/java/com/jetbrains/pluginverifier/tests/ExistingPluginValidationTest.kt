package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PluginProblem.Level.ERROR
import com.jetbrains.plugin.structure.base.problems.ReclassifiedPluginProblem
import com.jetbrains.plugin.structure.base.problems.unwrapped
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.problems.remapping.JsonUrlProblemLevelRemappingManager
import com.jetbrains.plugin.structure.intellij.problems.remapping.RemappingSet
import com.jetbrains.pluginverifier.options.PluginParsingConfiguration
import com.jetbrains.pluginverifier.options.PluginParsingConfigurationResolution
import com.jetbrains.pluginverifier.options.SubmissionType.EXISTING
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.*
import org.junit.Test
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class ExistingPluginValidationTest : BasePluginTest() {
  @Test
  fun `plugin is not built due to missing ID but such problem is filtered`() {
    val ideaPlugin = ideaPlugin(pluginId = "")
    val problemResolver = object : PluginCreationResultResolver {
      private val logger = LoggerFactory.getLogger("verification.structure")

      override fun resolve(plugin: IdePlugin, problems: List<PluginProblem>): PluginCreationResult<IdePlugin> {
        problems.forEach {
          logger.info("Plugin problem will be ignored by the problem resolver: $it")
        }
        return PluginCreationSuccess(plugin, emptyList())
      }

      override fun classify(
        plugin: IdePlugin,
        problems: List<PluginProblem>
      ): List<PluginProblem> {
        problems.forEach {
          logger.info("Plugin problem will be ignored by the problem resolver: $it")
        }
        return emptyList()
      }
    }

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $ideaPlugin
            </idea-plugin>
          """
        }
      }
    }
    assertTrue(result is PluginCreationSuccess)
    assertMatchingPluginProblems(result as PluginCreationSuccess)
  }

  @Test
  fun `plugin is not built due to unsupported prefix ID but such problem level is remapped`() {
    val header = ideaPlugin("com.example")
    val delegateResolver = IntelliJPluginCreationResultResolver()
    val problemResolver = LevelRemappingPluginCreationResultResolver(delegateResolver, warning<ForbiddenPluginIdPrefix>())

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $header
            </idea-plugin>
          """
        }
      }
    }
    assertTrue(result is PluginCreationSuccess)
    val pluginCreated = result as PluginCreationSuccess
    assertMatchingPluginProblems(result)

    assertEquals(1, pluginCreated.warnings.size)
    val reclassifiedPluginProblem = pluginCreated.warnings.first()
    assertEquals(PluginProblem.Level.WARNING, reclassifiedPluginProblem.level)
    assertTrue(reclassifiedPluginProblem is ReclassifiedPluginProblem)
    assertTrue((reclassifiedPluginProblem as ReclassifiedPluginProblem).unwrapped is ForbiddenPluginIdPrefix)
  }

  @Test
  fun `plugin is built and an error is remapped to an unnaceptable warning`() {
    val header = ideaPlugin("someId", untilBuild = "999")
    val delegateResolver = IntelliJPluginCreationResultResolver()
    val problemResolver = LevelRemappingPluginCreationResultResolver(delegateResolver,
      unacceptableWarning<InvalidUntilBuildWithMagicNumber>())

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $header
            </idea-plugin>
          """
        }
      }
    }
    assertTrue(result is PluginCreationSuccess)

    val pluginCreated = result as PluginCreationSuccess
    assertMatchingPluginProblems(result)

    assertEquals(1, pluginCreated.unacceptableWarnings.size)
    val reclassifiedPluginProblem = pluginCreated.unacceptableWarnings.first()
    assertEquals(PluginProblem.Level.UNACCEPTABLE_WARNING, reclassifiedPluginProblem.level)
    assertTrue(reclassifiedPluginProblem is ReclassifiedPluginProblem)
    assertTrue((reclassifiedPluginProblem as ReclassifiedPluginProblem).unwrapped is InvalidUntilBuildWithMagicNumber)
  }

  @Test
    fun `plugin is built with existing plugin verification rules`() {
    val problemResolver = getIntelliJPluginCreationResolver()

    val header = ideaPlugin("pluginverifier.intellij", "IntelliJ Plugin Verifier with Forbidden Word in Plugin Name")
    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $header
            </idea-plugin>
          """
        }
      }
    }
    assertTrue(result is PluginCreationSuccess)
    val pluginCreated = result as PluginCreationSuccess
    assertMatchingPluginProblems(result)

    assertEquals(0, pluginCreated.warnings.size)
    assertEquals(0, pluginCreated.unacceptableWarnings.size)
  }

  @Test
  fun `plugin is built with existing plugin verification rules and CLI-like remapping setup`() {
    val configuration = PluginParsingConfiguration(EXISTING, ignoredPluginProblems = emptyList())
    val problemResolver = PluginParsingConfigurationResolution()
      .resolveProblemLevelMapping(configuration, JsonUrlProblemLevelRemappingManager.fromClassPathJson())

    val header = ideaPlugin("com.intellij", "IntelliJ Plugin Verifier with Forbidden Word in Plugin Name")
    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $header
            </idea-plugin>
          """
        }
      }
    }
    assertTrue(result is PluginCreationSuccess)
    val pluginCreated = result as PluginCreationSuccess
    assertMatchingPluginProblems(result)

    assertEquals(0, pluginCreated.warnings.size)
    assertEquals(0, pluginCreated.unacceptableWarnings.size)
  }

  @Test
  fun `plugin and its optional dependency is built with existing plugin verification rules and CLI-like remapping setup`() {
    val configuration = PluginParsingConfiguration(EXISTING, ignoredPluginProblems = emptyList())
    val problemResolver = PluginParsingConfigurationResolution()
      .resolveProblemLevelMapping(configuration, JsonUrlProblemLevelRemappingManager.fromClassPathJson())

    val dependencyPluginId = "com.intellij.dep"
    val header = ideaPlugin("com.intellij", "IntelliJ Plugin Verifier with Forbidden Word in Plugin Name")
    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $header
              <depends optional="true" config-file="dependency.xml">$dependencyPluginId</depends>
            </idea-plugin>
          """
        }
        file("dependency.xml") {
          """
            <idea-plugin />
          """
        }
      }
    }

    assertTrue(result is PluginCreationSuccess)
    with(result as PluginCreationSuccess) {
      assertEquals(0, warnings.size)
      assertEquals(0, unacceptableWarnings.size)
    }

    assertMatchingPluginProblems(result)
  }

  @Test
  fun `plugin is built, it has two different plugin problems and both are remapped`() {
    val erroneousSinceBuild = "1.1"
    val header = ideaPlugin("com.example", sinceBuild = erroneousSinceBuild)
    val delegateResolver = IntelliJPluginCreationResultResolver()

    val problemResolver = LevelRemappingPluginCreationResultResolver(
      delegateResolver,
      warning<InvalidSinceBuild>() + warning<ForbiddenPluginIdPrefix>())

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $header
            </idea-plugin>
          """
        }
      }
    }
    assertThat("Plugin Creation Result must succeed", result, instanceOf(PluginCreationSuccess::class.java))
    val pluginCreated = result as PluginCreationSuccess
    assertMatchingPluginProblems(result)

    assertEquals(2, pluginCreated.warnings.size)
    val reclassifiedProblems = pluginCreated.warnings
      .filter { ReclassifiedPluginProblem::class.isInstance(it) }
      .map { it as ReclassifiedPluginProblem }
      .map { it.unwrapped }

    assertEquals(2, reclassifiedProblems.size)
    assertThat("Reclassified problems contains an 'IllegalPluginIdPrefix' plugin problem ", reclassifiedProblems.find { ForbiddenPluginIdPrefix::class.isInstance(it) } != null)
    assertThat("Reclassified problems contains an 'InvalidSinceBuild' plugin problem ", reclassifiedProblems.find { InvalidSinceBuild::class.isInstance(it) } != null)
  }

  @Test
  fun `plugin is built with forbidden word in plugin id but such problem is ignored`() {
    val header = ideaPlugin("com.vendor.qodana.plugin")

    val remappingProblemResolver = LevelRemappingPluginCreationResultResolver(
      IntelliJPluginCreationResultResolver(),
      ignore<TemplateWordInPluginId>())

    val result = buildPluginWithResult(remappingProblemResolver, pluginOf(header))
    assertThat("Plugin Creation Result must succeed", result, instanceOf(PluginCreationSuccess::class.java))
    val pluginCreated = result as PluginCreationSuccess
    assertMatchingPluginProblems(result)

    assertThat(pluginCreated.warnings, `is`(emptyList()))
  }

  @Test
  fun `plugin is built with forbidden word in plugin id`() {
    val header = ideaPlugin("com.vendor.qodana.plugin")

    val problemResolver = IntelliJPluginCreationResultResolver()
    val result = buildPluginWithResult(problemResolver, pluginOf(header))
    assertThat("Plugin Creation Result must succeed", result, instanceOf(PluginCreationSuccess::class.java))
    val pluginCreated = result as PluginCreationSuccess
    assertEquals(1, pluginCreated.warnings.size)

    val warning = pluginCreated.warnings.first()
    assertThat(warning, instanceOf(TemplateWordInPluginId::class.java))
  }

  @Test
  fun `plugin is not built due to two different plugin problems with 'error' severity but one level is remapped`() {
    val erroneousSinceBuild = "1.0"
    val erroneousUntilBuild = "1000"
    val header = ideaPlugin("plugin.with.two.problems", sinceBuild = erroneousSinceBuild, untilBuild = erroneousUntilBuild)
    val delegateResolver = IntelliJPluginCreationResultResolver()
    val problemResolver = LevelRemappingPluginCreationResultResolver(delegateResolver, warning<InvalidSinceBuild>())

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $header
            </idea-plugin>
          """
        }
      }
    }
    assertTrue(result is PluginCreationFail)
    val failure = result as PluginCreationFail
    assertEquals(2, failure.errorsAndWarnings.size)
    val reclassifiedProblems = failure.errorsAndWarnings
      .filter { ReclassifiedPluginProblem::class.isInstance(it) }
      .map { it as ReclassifiedPluginProblem }
      .map { it.unwrapped }

    assertEquals(1, reclassifiedProblems.size)
    assertThat("Reclassified problems contains an 'InvalidSinceBuild' plugin problem ", reclassifiedProblems.find { InvalidSinceBuild::class.isInstance(it) } != null)
  }

  @Test
  fun `plugin is built with two problems - an unacceptable warning and an error and both are remapped`() {
    val erroneousSinceBuild = "1.1"

    val header = ideaPlugin("plugin.with.error.and.unacceptable.warning",
      sinceBuild = erroneousSinceBuild,
      description = "<![CDATA[A failing plugin with HTTP link leading to <a href='http://jetbrains.com'>JetBrains</a>]]>")

    val problemResolver = LevelRemappingPluginCreationResultResolver(IntelliJPluginCreationResultResolver(),
      warning<HttpLinkInDescription>() + ignore<InvalidSinceBuild>()
    )

    val result = buildPluginWithResult(problemResolver, pluginOf(header))

    // InvalidSinceBuild is an ignored ERROR, hence leading to a creation success
    assertTrue(result is PluginCreationSuccess)
    val success = result as PluginCreationSuccess

    // HttpLinkInDescription has a decreased severity from UNACCEPTABLE_WARNING to the WARNING
    assertEquals(1, success.warnings.size)
    val reclassifiedProblems = success.warnings
      .filter { ReclassifiedPluginProblem::class.isInstance(it) }
      .map { it as ReclassifiedPluginProblem }
      .map { it.unwrapped }

    assertEquals(1, reclassifiedProblems.size)
    assertThat("Reclassified problems contains an 'HttpLinkInDescription' plugin problem ", reclassifiedProblems.find { HttpLinkInDescription::class.isInstance(it) } != null)

    assertThat(success.unacceptableWarnings, `is`(emptyList()))

  }

  @Test
  fun `plugin is built with two problems and 'error' is reclassified to 'unacceptable warning' thus being successful`() {
    val erroneousSinceBuild = "1.*"

    val header = ideaPlugin("plugin.with.error.and.unacceptable.warning",
      sinceBuild = erroneousSinceBuild,
      description = "<![CDATA[A failing plugin with HTTP link leading to <a href='http://jetbrains.com'>JetBrains</a>]]>")

    val problemResolver = LevelRemappingPluginCreationResultResolver(IntelliJPluginCreationResultResolver(),
      unacceptableWarning<InvalidSinceBuild>())

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $header
            </idea-plugin>
          """
        }
      }
    }

    assertTrue(result is PluginCreationSuccess)
    val success = result as PluginCreationSuccess
    assertEquals(2, success.unacceptableWarnings.size)
    val reclassifiedProblems = success.unacceptableWarnings
      .filter { ReclassifiedPluginProblem::class.isInstance(it) }
      .map { it as ReclassifiedPluginProblem }
      .map { it.unwrapped }

    assertEquals(1, reclassifiedProblems.size)
    assertThat("Reclassified problems contains an 'InvalidSinceBuild' plugin problem ", reclassifiedProblems.find { InvalidSinceBuild::class.isInstance(it) } != null)
  }

  @Test
  fun `plugin is not built due to a warning and such problem level is escalated to an error`() {
    val suspiciousUntilBuild = "291.1" // 2029.1.1
    val header = ideaPlugin("com.example", untilBuild = suspiciousUntilBuild)
    val delegateResolver = IntelliJPluginCreationResultResolver()
    val problemResolver = LevelRemappingPluginCreationResultResolver(delegateResolver, error<SuspiciousUntilBuild>())

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $header
            </idea-plugin>
          """
        }
      }
    }
    assertThat(result, instanceOf(PluginCreationFail::class.java))
    val creationFailure = result as PluginCreationFail
    assertEquals(2, creationFailure.errorsAndWarnings.size)
    val reclassifiedPluginProblem = creationFailure.errorsAndWarnings.first { it.level == ERROR }
    assertEquals(ERROR, reclassifiedPluginProblem.level)
    assertThat(reclassifiedPluginProblem, instanceOf(ReclassifiedPluginProblem::class.java))
    assertTrue((reclassifiedPluginProblem as ReclassifiedPluginProblem).unwrapped is SuspiciousUntilBuild)
  }

  @Test
  fun `internal plugin is built despite having an descriptor error because it is remapped`() {
    // Intentional JetBrains plugin that contains a forbidden word in the plugin name
    val header = ideaPlugin("com.jetbrains.SomePlugin", "IDEA Fountain", vendor = "JetBrains")
    val delegateResolver = IntelliJPluginCreationResultResolver()

    val levelRemappingDefinition = JsonUrlProblemLevelRemappingManager.fromClassPathJson().load()
    val jetBrainsPluginLevelRemapping = levelRemappingDefinition.getOrEmpty(RemappingSet.JETBRAINS_PLUGIN_REMAPPING_SET)
    val problemResolver = JetBrainsPluginCreationResultResolver(
      LevelRemappingPluginCreationResultResolver(delegateResolver, error<TemplateWordInPluginName>()),
      jetBrainsPluginLevelRemapping)

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $header
            </idea-plugin>
          """
        }
      }
    }
    assertThat(result, instanceOf(PluginCreationSuccess::class.java))
    val creationSuccess = result as PluginCreationSuccess
    assertMatchingPluginProblems(result)

    assertThat(creationSuccess.unacceptableWarnings.size, `is`(0))
    assertThat(creationSuccess.warnings.size, `is`(0))
  }

  @Test
  fun `internal plugin is build despite having a service extension point preload because it is remapped`() {
    val header = ideaPlugin(vendor = "JetBrains")
    val delegateResolver = IntelliJPluginCreationResultResolver()

    val levelRemappingDefinition = JsonUrlProblemLevelRemappingManager.fromClassPathJson().load()
    val jetBrainsPluginLevelRemapping = levelRemappingDefinition.getOrEmpty(RemappingSet.JETBRAINS_PLUGIN_REMAPPING_SET)
    val problemResolver = JetBrainsPluginCreationResultResolver(
      LevelRemappingPluginCreationResultResolver(delegateResolver, error<TemplateWordInPluginName>()),
      jetBrainsPluginLevelRemapping
    )

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
          <idea-plugin>
            $header
            <extensions defaultExtensionNs="com.intellij">
              <applicationService
                  serviceInterface="com.example.MyAppService"
                  serviceImplementation="com.example.MyAppServiceImpl"
                  preload="await"
                  />
            </extensions>  
          </idea-plugin>
        """
        }
      }
    }
    assertThat(result, instanceOf(PluginCreationSuccess::class.java))
    val creationSuccess = result as PluginCreationSuccess

    assertMatchingPluginProblems(result)
    assertThat(creationSuccess.unacceptableWarnings.size, `is`(0))
    // warning about ServiceExtensionPointPreload
    val warnings = creationSuccess.warnings
    assertEquals(1, warnings.size)
    val warning = warnings.map { it.unwrapped }.filterIsInstance<ServiceExtensionPointPreloadNotSupported>()
      .singleOrNull()
    assertNotNull("Expected 'Service Extension Point Preload Not Supported' plugin error", warning)
  }

  @Test
  fun `internal plugin is built despite having release date in the future because it is remapped`() {
    val releaseDateInFuture = LocalDate.now().plusMonths(1)
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    val releaseDateInFutureString = releaseDateInFuture.format(formatter)
    val header = ideaPlugin()
    val problemResolver = getIntelliJPluginCreationResolver(isExistingPlugin = false, isJetBrainsPlugin = true)

    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
          <idea-plugin>
            $header
            <product-descriptor code="ABC" release-date="$releaseDateInFutureString" release-version="12"/>
          </idea-plugin>
        """
        }
      }
    }
    assertThat(result, instanceOf(PluginCreationSuccess::class.java))
    val creationSuccess = result as PluginCreationSuccess
    assertMatchingPluginProblems(result)

    assertThat(creationSuccess.unacceptableWarnings.size, `is`(0))
    assertThat(creationSuccess.warnings.size, `is`(1))
    val warning = creationSuccess.warnings.map { it.unwrapped }.filterIsInstance<ReleaseDateInFuture>()
      .singleOrNull()
    assertNotNull("Expected 'ReleaseDateInFuture' plugin warning", warning)
  }

  @Test
  fun `paid plugin is not built due to invalid release-version but such problem is filtered because it is an existing plugin`() {
    val singleDigitReleaseVersion = "1"
    val paidIdeaPlugin = paidIdeaPlugin(releaseVersion = singleDigitReleaseVersion)
    val problemResolver = getIntelliJPluginCreationResolver(isExistingPlugin = true)
    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $paidIdeaPlugin
            </idea-plugin>
          """
        }
      }
    }
    assertSuccess(result)
    assertMatchingPluginProblems(result as PluginCreationSuccess)
  }

  @Test
  fun `existing paid plugin has release-version that does not match plugin version and this is an error`() {
    val paidIdeaPlugin = paidIdeaPlugin(pluginVersion = "2.1", releaseVersion = "20")
    val problemResolver = getIntelliJPluginCreationResolver(isExistingPlugin = true)
    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $paidIdeaPlugin
            </idea-plugin>
          """
        }
      }
    }
    result.assertContains<ReleaseVersionAndPluginVersionMismatch>("Invalid plugin descriptor 'plugin.xml'. " +
      "The <release-version> parameter [20] and the plugin version [2.1] should have a matching beginning. " +
      "For example, release version '20201' should match plugin version 2020.1.1")
  }

  @Test
  fun `existing paid plugin has release-version that is zero-bases (03)`() {
    val paidIdeaPlugin = paidIdeaPlugin(pluginVersion = "0.3.333", releaseVersion = "03")
    val problemResolver = getIntelliJPluginCreationResolver(isExistingPlugin = true)
    val result = buildPluginWithResult(problemResolver) {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $paidIdeaPlugin
            </idea-plugin>
          """
        }
      }
    }
    assertSuccess(result)
    result.assertContainsWarning<ReleaseVersionWrongFormat>("Invalid plugin descriptor 'plugin.xml'. " +
      "The <release-version> parameter (03) format is invalid. " +
      "Ensure it is an integer with at least two digits.")
  }

  private fun pluginOf(header: String): ContentBuilder.() -> Unit = {
    dir("META-INF") {
      file("plugin.xml") {
        """
          <idea-plugin>
            $header
          </idea-plugin>
        """
      }
    }
  }

  private fun paidIdeaPlugin(pluginId: String = "someid",
                         pluginName: String = "someName",
                         pluginVersion: String = "1",
                         vendor: String = "vendor",
                         sinceBuild: String = "131.1",
                         untilBuild: String = "231.1",
                         description: String = "this description is looooooooooong enough",
                         releaseVersion: String = "20211") =
    ideaPlugin(pluginId, pluginName, pluginVersion, vendor, sinceBuild, untilBuild, description) +
      """
        <product-descriptor code="PTESTPLUGIN" release-date="20210818" release-version="$releaseVersion"/>
      """.trimIndent()

  private fun getIntelliJPluginCreationResolver(isExistingPlugin: Boolean = true, isJetBrainsPlugin: Boolean = false): PluginCreationResultResolver {
      JsonUrlProblemLevelRemappingManager.fromClassPathJson().let {
          val defaultResolver = if (isExistingPlugin) {
              it.defaultExistingPluginResolver()
          } else {
              it.defaultNewPluginResolver()
          }
          return if (isJetBrainsPlugin) {
              it.defaultJetBrainsPluginResolver(defaultResolver)
          } else {
              defaultResolver
          }
      }
  }
}