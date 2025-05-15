package com.jetbrains.plugin.structure.intellij

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.isInstance
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.KotlinPluginMode
import com.jetbrains.plugin.structure.intellij.plugin.ModuleV2Dependency
import com.jetbrains.plugin.structure.intellij.plugin.PluginV2Dependency
import com.jetbrains.plugin.structure.intellij.problems.ModuleDescriptorResolutionProblem
import com.jetbrains.plugin.structure.intellij.problems.NoDependencies
import com.jetbrains.plugin.structure.intellij.problems.NoModuleDependencies
import com.jetbrains.plugin.structure.intellij.problems.OptionalDependencyConfigFileIsEmpty
import com.jetbrains.plugin.structure.intellij.problems.OptionalDependencyConfigFileNotSpecified
import com.jetbrains.plugin.structure.intellij.problems.ProhibitedModuleExposed
import com.jetbrains.plugin.structure.intellij.problems.ReleaseVersionAndPluginVersionMismatch
import com.jetbrains.plugin.structure.intellij.problems.ReleaseVersionWrongFormat
import com.jetbrains.plugin.structure.intellij.problems.ServiceExtensionPointPreloadNotSupported
import com.jetbrains.plugin.structure.intellij.problems.UndeclaredKotlinK2CompatibilityMode
import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedReader
import java.io.IOException
import java.util.*
import kotlin.reflect.KClass

private const val HEADER = """
      <id>someId</id>
      <name>someName</name>
      <version>someVersion</version>
      ""<vendor email="vendor.com" url="url">vendor</vendor>""
      <description>this description is looooooooooong enough</description>
      <change-notes>these change-notes are looooooooooong enough</change-notes>
      <idea-version since-build="131.1"/>
    """

private const val HEADER_WITHOUT_VERSION = """
      <id>someId</id>
      <name>someName</name>
      <vendor email="vendor.com" url="url">vendor</vendor>
      <description>this description is looooooooooong enough</description>
      <change-notes>these change-notes are looooooooooong enough</change-notes>
      <idea-version since-build="131.1"/>
    """

private const val JETBRAINS_PLUGIN_HEADER = """
      <id>com.jetbrains.SomePlugin</id>
      <name>someName</name>
      <version>someVersion</version>
      <vendor email="example@jetbrains.com" url="https://www.jetbrains.com/">"JetBrains s.r.o."</vendor>
      <description>this description is looooooooooong enough</description>
      <change-notes>these change-notes are looooooooooong enough</change-notes>
      <idea-version since-build="131.1"/>
    """

private const val PLUGIN_JAR_NAME = "plugin.jar"

class PluginXmlValidationTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `plugin declaring optional dependency but missing config-file`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <depends>com.intellij.modules.platform</depends>
              <depends optional="true">com.intellij.bundled.plugin.id</depends>
            </idea-plugin>
          """
        }
      }
    }

    val warnings = pluginCreationSuccess.warnings
    assertEquals(1, warnings.size)
    val warning = warnings.filterIsInstance<OptionalDependencyConfigFileNotSpecified>()
            .singleOrNull()
    assertNotNull("Expected 'Optional Dependency Config File Not Specified' plugin warning", warning)
  }

  @Test
  fun `plugin declaring optional dependency but empty config-file is a creation error`() {
    val pluginCreationFail = buildMalformedPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <depends>com.intellij.modules.platform</depends>
              <depends optional="true" config-file="">com.intellij.optional.plugin.id</depends>
            </idea-plugin>
          """
        }
      }
    }

    val errorsAndWarnings = pluginCreationFail.errorsAndWarnings
    assertEquals(1, errorsAndWarnings.size)
    val error = errorsAndWarnings.filterIsInstance<OptionalDependencyConfigFileIsEmpty>()
            .singleOrNull()
    assertNotNull("Expected 'Optional Dependency Config File Is Empty' plugin warning", error)
  }

  @Test
  fun `plugin declaring projectService with preloading should emit an unacceptable warning`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <depends>com.intellij.modules.platform</depends>
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

    val warnings = pluginCreationSuccess.allWarnings
    assertEquals(1, warnings.size)
    val error = warnings.filterIsInstance<ServiceExtensionPointPreloadNotSupported>()
      .singleOrNull()
    assertNotNull("Expected 'Service Extension Point Preload Not Supported' plugin error", error)
    assertEquals(PluginProblem.Level.UNACCEPTABLE_WARNING, error?.level)
  }

  @Test
  fun `non-v2 plugin without dependencies error`() {
    val pluginCreationFail = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
            </idea-plugin>
          """
        }
      }
    }

    val unacceptableWarnings = pluginCreationFail.unacceptableWarnings
    assertEquals(1, unacceptableWarnings.size)
    val unacceptableWarning = unacceptableWarnings.filterIsInstance<NoDependencies>()
      .singleOrNull()
    assertNotNull("Plugin descriptor plugin.xml does not include any module dependency tags. The plugin is assumed to be a legacy plugin and is loaded only in IntelliJ IDEA. See https://plugins.jetbrains.com/docs/intellij/plugin-compatibility.html", unacceptableWarning)
  }
  
  @Test
  fun `plugin with v2 dependency on plugin`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <dependencies>
                <plugin id="org.jetbrains.kotlin"/>
              </dependencies>
            </idea-plugin>
          """
        }
      }
    }

    assertEquals(emptyList<PluginProblem>(), pluginCreationSuccess.unacceptableWarnings)
    assertEquals(emptyList<PluginProblem>(), pluginCreationSuccess.warnings.filterIsInstance<NoModuleDependencies>())
  }

  @Test
  fun `plugin IDE mode compatibility for K1 and K2`() {
    val scenarios = mapOf(
      "" to KotlinPluginMode.Implicit,
      "<supportsKotlinPluginMode supportsK1='true' supportsK2='true' />" to KotlinPluginMode.K1AndK2Compatible,
      "<supportsKotlinPluginMode supportsK1='true' supportsK2='false' />" to KotlinPluginMode.K1OnlyCompatible,
      "<supportsKotlinPluginMode supportsK1='false' supportsK2='true' />" to KotlinPluginMode.K2OnlyCompatible,
    )

    scenarios.forEach { (extensionXml, expectedIdeMode) ->
      val pluginJar = "plugin${UUID.randomUUID()}.jar"
      val pluginCreationSuccess = buildCorrectPlugin(pluginJar) {
        dir("META-INF") {
          file("plugin.xml") {
            """
            <idea-plugin>
              $HEADER
              <extensions defaultExtensionNs="org.jetbrains.kotlin">
                  $extensionXml
              </extensions>
            </idea-plugin>
          """
          }
        }
      }
      assertEquals(expectedIdeMode, pluginCreationSuccess.plugin.kotlinPluginMode)
    }
  }

  @Test
  fun `paid plugin`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER_WITHOUT_VERSION
              <version>1.1.0</version>
              <product-descriptor code="PCODE" release-date="20240813" release-version="11"/>
              <depends>com.intellij.modules.lang</depends>              
            </idea-plugin>
          """
        }
      }
    }
    val plugin = pluginCreationSuccess.plugin
    assertNotNull(plugin.productDescriptor)
    with(plugin.productDescriptor!!) {
      assertEquals("PCODE", code)
      assertEquals(java.time.LocalDate.of(2024, 8, 13), releaseDate)
      assertEquals(ProductReleaseVersion(11), version)
    }
  }

  @Test
  fun `paid plugin with an incorrect single-digit release version`() {
    val creationResult = buildMalformedPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER_WITHOUT_VERSION
              <version>1.1</version>
              <product-descriptor code="PCODE" release-date="20240813" release-version="1"/>
              <depends>com.intellij.modules.lang</depends>              
            </idea-plugin>
          """
        }
      }
    }
    with(creationResult.errorsAndWarnings) {
      assertEquals(1, size)
      val problem = filterIsInstance<ReleaseVersionWrongFormat>()
        .singleOrNull()
      assertNotNull(problem)
      problem!!
      assertEquals("Invalid plugin descriptor 'plugin.xml'. The <release-version> parameter (1) format is invalid. Ensure it is an integer with at least two digits.", problem.toString())
    }
  }

  @Test
  fun `paid plugin with a mismatching release version and plugin version`() {
    val creationResult = buildMalformedPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER_WITHOUT_VERSION
              <version>2.0</version>
              <product-descriptor code="PCODE" release-date="20240813" release-version="10"/>
              <depends>com.intellij.modules.lang</depends>              
            </idea-plugin>
          """
        }
      }
    }
    with(creationResult.errorsAndWarnings) {
      assertEquals(1, size)
      val problem = filterIsInstance<ReleaseVersionAndPluginVersionMismatch>().singleOrNull()
      assertNotNull(problem)
      problem!!
      assertEquals("Invalid plugin descriptor 'plugin.xml'. The <release-version> parameter [10] and the plugin version [2.0] should have a matching beginning. For example, release version '20201' should match plugin version 2020.1.1", problem.toString())
    }
  }

  @Test
  fun `paid plugin with a mismatching release version and plugin version in minor components`() {
    val creationResult = buildMalformedPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER_WITHOUT_VERSION
              <version>2020.2.1</version>
              <product-descriptor code="PCODE" release-date="20240813" release-version="20201"/>
              <depends>com.intellij.modules.lang</depends>              
            </idea-plugin>
          """
        }
      }
    }
    with(creationResult.errorsAndWarnings) {
      assertEquals(1, size)
      val problem = filterIsInstance<ReleaseVersionAndPluginVersionMismatch>().singleOrNull()
      assertNotNull(problem)
      problem!!
      assertEquals("Invalid plugin descriptor 'plugin.xml'. The <release-version> parameter [20201] and the plugin version [2020.2.1] should have a matching beginning. For example, release version '20201' should match plugin version 2020.1.1", problem.toString())
    }
  }

  @Test
  fun `plugin depends on the Kotlin plugin but does not declare IDE mode compatibility`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <depends>com.intellij.modules.platform</depends>
              <depends>org.jetbrains.kotlin</depends>
            </idea-plugin>
          """
        }
      }
    }
    pluginCreationSuccess.assertContains<UndeclaredKotlinK2CompatibilityMode>(
      "Invalid plugin descriptor 'plugin.xml'. " +
        "Plugin depends on the Kotlin plugin (org.jetbrains.kotlin) but does not declare " +
        "a compatibility mode in the <org.jetbrains.kotlin.supportsKotlinPluginMode> extension. " +
        "This feature is available for IntelliJ IDEA 2024.2.1 or later."
    )
  }


  @Test
  fun `plugin declares a single content module in CDATA`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml", classpath("/descriptors/ml-llm/plugin-single-module-in-cdata.xml"))
      }
    }

    assertEquals(emptyList<PluginProblem>(), pluginCreationSuccess.unacceptableWarnings)
    
    pluginCreationSuccess.warnings.filterIsInstance<ModuleDescriptorResolutionProblem>().let {
      assertEquals(0, it.size)
    }
    with(pluginCreationSuccess.plugin.modulesDescriptors) {
      assertEquals(1, size)
      val intellijMlLlmPrivacy = first()
      assertEquals("intellij.ml.llm.privacy", intellijMlLlmPrivacy.name)
      val privacyDeps = intellijMlLlmPrivacy.dependencies
      assertEquals(1, privacyDeps.size)
      val platformVcsImpl = privacyDeps.first()
      assertTrue(platformVcsImpl is ModuleV2Dependency)
      platformVcsImpl as ModuleV2Dependency
      assertEquals("intellij.platform.vcs.impl", platformVcsImpl.id)
    }

  }

  @Test
  fun `plugin declares multiple modules in CDATA`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml", classpath("/descriptors/ml-llm/plugin-modules-in-cdata.xml"))
      }
    }
    val modules = pluginCreationSuccess.plugin.modulesDescriptors
    assertEquals(86, modules.size)
    val moduleNames = modules.map { it.name }.sorted()
    val expectedModuleNames = listOf(
      "intellij.ml.llm.android",
      "intellij.ml.llm.chatInputLanguage",
      "intellij.ml.llm.chatInputLanguage.grazie",
      "intellij.ml.llm.completion",
      "intellij.ml.llm.core",
      "intellij.ml.llm.cpp",
      "intellij.ml.llm.cpp.common",
      "intellij.ml.llm.cpp.completion",
      "intellij.ml.llm.css.completion",
      "intellij.ml.llm.css.less.completion",
      "intellij.ml.llm.css.postcss.completion",
      "intellij.ml.llm.css.sass.completion",
      "intellij.ml.llm.devkit",
      "intellij.ml.llm.domains",
      "intellij.ml.llm.domains.ide",
      "intellij.ml.llm.embeddings",
      "intellij.ml.llm.embeddings.core",
      "intellij.ml.llm.embeddings.java",
      "intellij.ml.llm.embeddings.kotlin",
      "intellij.ml.llm.embeddings.python",
      "intellij.ml.llm.embeddings.searchEverywhere",
      "intellij.ml.llm.embeddings.smartChat",
      "intellij.ml.llm.embeddings.testCommands",
      "intellij.ml.llm.experiments",
      "intellij.ml.llm.experiments.platformAb",
      "intellij.ml.llm.github",
      "intellij.ml.llm.gitlab",
      "intellij.ml.llm.go",
      "intellij.ml.llm.go.completion",
      "intellij.ml.llm.go.inlinePromptDetector",
      "intellij.ml.llm.groovy.inlinePromptDetector",
      "intellij.ml.llm.html.completion",
      "intellij.ml.llm.httpClient",
      "intellij.ml.llm.impl",
      "intellij.ml.llm.inlinePromptDetector",
      "intellij.ml.llm.java",
      "intellij.ml.llm.java.completion",
      "intellij.ml.llm.java.inlinePromptDetector",
      "intellij.ml.llm.javaee",
      "intellij.ml.llm.javascript",
      "intellij.ml.llm.javascript.astro.completion",
      "intellij.ml.llm.javascript.completion",
      "intellij.ml.llm.javascript.inlinePromptDetector",
      "intellij.ml.llm.javascript.svelte.completion",
      "intellij.ml.llm.javascript.vue",
      "intellij.ml.llm.javascript.vue.completion",
      "intellij.ml.llm.jupyter.common",
      "intellij.ml.llm.jupyter.kotlin",
      "intellij.ml.llm.jupyter.python",
      "intellij.ml.llm.jupyter.python.completion",
      "intellij.ml.llm.kotlin",
      "intellij.ml.llm.kotlin.completion",
      "intellij.ml.llm.kotlin.inlinePromptDetector",
      "intellij.ml.llm.latest",
      "intellij.ml.llm.markdown",
      "intellij.ml.llm.microservices",
      "intellij.ml.llm.performanceTesting",
      "intellij.ml.llm.php",
      "intellij.ml.llm.php.completion",
      "intellij.ml.llm.php.inlinePromptDetector",
      "intellij.ml.llm.privacy",
      "intellij.ml.llm.provider.ollama",
      "intellij.ml.llm.python",
      "intellij.ml.llm.python.completion",
      "intellij.ml.llm.python.inlinePromptDetector",
      "intellij.ml.llm.python.ultimate",
      "intellij.ml.llm.rider",
      "intellij.ml.llm.rider.cpp",
      "intellij.ml.llm.rider.cpp.completion",
      "intellij.ml.llm.rider.csharp",
      "intellij.ml.llm.rider.csharp.completion",
      "intellij.ml.llm.rider.csharp.razor",
      "intellij.ml.llm.ruby",
      "intellij.ml.llm.ruby.completion",
      "intellij.ml.llm.ruby.inlinePromptDetector",
      "intellij.ml.llm.sh",
      "intellij.ml.llm.sql",
      "intellij.ml.llm.sql.completion",
      "intellij.ml.llm.sql/embeddings",
      "intellij.ml.llm.structuralSearch",
      "intellij.ml.llm.terminal",
      "intellij.ml.llm.terraform.completion",
      "intellij.ml.llm.textmate",
      "intellij.ml.llm.uiTestGeneration",
      "intellij.ml.llm.vcs",
      "intellij.ml.llm.yaml.inlinePromptDetector"
    )
    assertEquals(expectedModuleNames, moduleNames)
  }

  @Test
  fun `content dependencies in plugin that declares multiple modules are resolved`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml", classpath("/descriptors/ml-llm/plugin-modules-in-cdata.xml"))
      }
    }
    val modules = pluginCreationSuccess.plugin.modulesDescriptors
    val llmCoreDeps = modules.first { it.name == "intellij.ml.llm.core" }.dependencies
    assertEquals(4, llmCoreDeps.size)
    assertTrue(llmCoreDeps.contains(ModuleV2Dependency("intellij.ml.llm.privacy")))
    assertTrue(llmCoreDeps.contains(ModuleV2Dependency("intellij.libraries.ktor.client")))
    assertTrue(llmCoreDeps.contains(PluginV2Dependency("com.intellij.platform.ide.provisioner")))
    assertTrue(llmCoreDeps.contains(PluginV2Dependency("com.intellij.llmInstaller")))
  }

  @Test
  fun `modules and plugin from plugin v2 'dependencies' tag are required`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <dependencies>
                <module name="intellij.dev.psiViewer" />
                <plugin id="org.intellij.plugins.markdown" />
              </dependencies>
            </idea-plugin>
          """
        }
      }
    }
    with(pluginCreationSuccess.plugin.dependencies) {
      assertEquals(2, size)
      assertTrue(all { !it.isOptional })
    }
  }

  @Test
  fun `JetBrains plugin exposes com_intellij module`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $JETBRAINS_PLUGIN_HEADER
              <module value="com.intellij.modules.json"/>
            </idea-plugin>
          """
        }
      }
    }
    with(pluginCreationSuccess.plugin) {
      assertEquals(setOf("com.intellij.modules.json"), definedModules)
    }
  }

  @Test
  fun `non-JetBrains plugin exposes com_intellij module`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <depends>com.intellij.modules.platform</depends>              
              <module value="com.intellij.modules.json"/>
            </idea-plugin>
          """
        }
      }
    }
    with(pluginCreationSuccess) {
      assertTrue(warnings.isEmpty())
      assertContains<ProhibitedModuleExposed>("Invalid plugin descriptor 'plugin.xml'. " +
        "Plugin declares a module with prohibited name: 'com.intellij.modules.json' has prefix 'com.intellij'. " +
        "Such modules cannot be declared by third party plugins.")
    }
  }

  @Test
  fun `non-JetBrains plugin exposes multiple internal modules`() {
    val pluginCreationSuccess = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml") {
          """
            <idea-plugin>
              $HEADER
              <depends>com.intellij.modules.platform</depends>              
              <module value="com.intellij.modules.json"/>
              <module value="org.jetbrains.plugins.vue"/>
              <module value="intellij.fullLine.java"/>
            </idea-plugin>
          """
        }
      }
    }
    with(pluginCreationSuccess) {
      assertTrue(warnings.isEmpty())
      assertContains<ProhibitedModuleExposed>(
        "Invalid plugin descriptor 'plugin.xml'. " +
          "Plugin declares 3 modules with prohibited names: " +
          "'intellij.fullLine.java' has prefix 'intellij', " +
          "'com.intellij.modules.json' has prefix 'com.intellij', " +
          "'org.jetbrains.plugins.vue' has prefix 'org.jetbrains'. " +
          "Such modules cannot be declared by third party plugins."
      )
    }
  }

  private fun buildMalformedPlugin(pluginJarName: String = PLUGIN_JAR_NAME, pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationFail<IdePlugin> {
    val pluginCreationResult = buildIdePlugin(pluginJarName, pluginContentBuilder)
    if (pluginCreationResult !is PluginCreationFail) {
      fail("This plugin was expected to fail during creation, but the creation process was successful." +
              " Please ensure that this is the intended behavior in the unit test.")
    }
    return pluginCreationResult as PluginCreationFail
  }

  private fun buildCorrectPlugin(pluginJarName: String = PLUGIN_JAR_NAME, pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationSuccess<IdePlugin> {
    val pluginCreationResult = buildIdePlugin(pluginJarName, pluginContentBuilder)
    if (pluginCreationResult !is PluginCreationSuccess) {
      fail("This plugin has not been created. Creation failed with error(s).")
    }
    return pluginCreationResult as PluginCreationSuccess
  }

  private fun buildIdePlugin(pluginJarName: String = PLUGIN_JAR_NAME, pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile(pluginJarName).toPath(), pluginContentBuilder)
    return IdePluginManager.createManager().createPlugin(pluginFile)
  }

  val PluginCreationSuccess<IdePlugin>.allWarnings
    get() = warnings + unacceptableWarnings

  private fun assertSuccess(pluginResult: PluginCreationResult<IdePlugin>) {
    when (pluginResult) {
      is PluginCreationSuccess -> return
      is PluginCreationFail -> with(pluginResult.errorsAndWarnings) {
        fail("Expected successful plugin creation, but got $size problem(s): "
          + joinToString { it.message })
      }
    }
  }

  private inline fun <reified T : PluginProblem> PluginCreationResult<IdePlugin>.assertContains(message: String) {
    val problems = when (this) {
      is PluginCreationSuccess -> warnings + unacceptableWarnings
      is PluginCreationFail -> errorsAndWarnings
    }
    assertContains(problems, T::class, message)
  }

  private fun assertContains(
    pluginProblems: Collection<PluginProblem>,
    pluginProblemClass: KClass<out PluginProblem>,
    message: String
  ) {
    val problems = pluginProblems.filter { problem ->
      problem.isInstance(pluginProblemClass)
    }
    if (problems.isEmpty()) {
      fail("Plugin creation result does not contain any problem of class [${pluginProblemClass.qualifiedName}]")
      return
    }
    val problemsWithMessage = problems.filter { it.message == message }
    if (problemsWithMessage.isEmpty()) {
      fail("Plugin creation result has ${problems.size} problem of class [${pluginProblemClass.qualifiedName}], " +
        "but none has a message '$message'. " +
        "Found [" + problems.joinToString { it.message } + "]"
      )
      return
    }
  }

  @Throws(IOException::class)
  private fun classpath(classPathResource: String): String {
    PluginXmlValidationTest::class.java.getResourceAsStream(classPathResource)?.use {
      return it.bufferedReader(Charsets.UTF_8).use(BufferedReader::readText)
    } ?: throw IOException("Cannot resolve [$classPathResource] in classpath")
  }
}
