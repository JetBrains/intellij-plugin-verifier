package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.ide.MockIdeBuilder
import com.jetbrains.plugin.structure.ide.ProductInfoBasedIdeManager
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.Dependency
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.DependencyTree
import com.jetbrains.plugin.structure.intellij.plugin.dependencies.PluginAware
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import com.jetbrains.plugin.structure.mocks.SimplePluginCreatorResultResolver
import org.intellij.lang.annotations.Language
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class InlineModuleTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ide: Ide

  private val pluginProblemRemapper = SimplePluginCreatorResultResolver()

  @Before
  fun setUp() {
    val ideRoot = MockIdeBuilder(temporaryFolder).buildIdeaDirectory()
    val ideManager = ProductInfoBasedIdeManager()
    this.ide = ideManager.createIde(ideRoot)
  }

  @Test
  fun `plugin with inline module is handled`() {
    val plugin = buildCorrectPlugin {
        dir("META-INF") {
          file("plugin.xml", pluginXmlContent)
        }
    }.plugin

    val modules = plugin.modulesDescriptors
    assertEquals(1, modules.size)
    val structureSearchModule = modules.first().module
    with(structureSearchModule.dependencies) {
      assertEquals(1, size)
      assertEquals("com.intellij.modules.structuralsearch", first().id)
    }
    with(structureSearchModule.extensions) {
      assertEquals(2, size)
    }
    with(structureSearchModule.appContainerDescriptor.services) {
      assertEquals(1, size)
      with(first()) {
        assertEquals("com.intellij.structuralsearch.plugin.ui.StructuralSearchTemplateBuilder", serviceInterface)
        assertEquals("com.intellij.structuralsearch.java.ui.JavaStructuralSearchTemplateBuilder", serviceImplementation)
      }
    }
  }

  @Test
  fun `transitive dependencies of the plugin with inline module are handled`() {
    val plugin = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml", pluginXmlContent)
      }
    }.plugin

    val idePlugins = listOf(
      MockIdePlugin(pluginId = "com.intellij.modules.lang"),
      MockIdePlugin(
        pluginId = "com.intellij.modules.idea.community",
        // fictional dependency that will become a transitive one
        dependencies = listOf(PluginDependencyImpl("com.intellij.transitiveDependency", false, false))
      ),
      MockIdePlugin(pluginId = "com.intellij.modules.structuralsearch"),
      // fictional plugin that poses as a transitional dependency
      MockIdePlugin(pluginId = "com.intellij.transitiveDependency")
    )
    val ideVersionStr = "IU-242.10180.25"
    val ide = MockIde(IdeVersion.createIdeVersion(ideVersionStr), temporaryFolder.newFolder(ideVersionStr).toPath(), idePlugins)

    val dependencyTree = DependencyTree(ide)
    with(dependencyTree.getTransitiveDependencies(plugin)) {
      assertEquals(4, size)
      // optional v1 dependency
      assertContains("com.intellij.modules.idea.community")
      // optional v2 dependency
      assertContains("com.intellij.modules.lang")
      // dependency declared in a content module
      assertContains("com.intellij.modules.structuralsearch")
      // transitive dependency, declared by bundled plugin
      assertContains("com.intellij.modules.transitiveDependency")
    }
  }

  @Test
  fun `transitive dependencies of the plugin with inline module are handled but some declared dependencies are missing`() {
    val plugin = buildCorrectPlugin {
      dir("META-INF") {
        file("plugin.xml", pluginXmlContent)
      }
    }.plugin

    val idePlugins = listOf(
      MockIdePlugin(pluginId = "com.intellij.modules.lang"),
      MockIdePlugin(pluginId = "com.intellij.modules.structuralsearch"),
    )
    val ideVersionStr = "IU-242.10180.25"
    val ide = MockIde(IdeVersion.createIdeVersion(ideVersionStr), temporaryFolder.newFolder(ideVersionStr).toPath(), idePlugins)

    with(plugin.dependencies) {
      assertEquals(3, size)
      // optional v1 dependency
      assertContains("com.intellij.modules.idea.community")
      // optional v2 dependency
      assertContains("com.intellij.modules.lang")
      // dependency declared in a content module
      assertContains("com.intellij.modules.structuralsearch")
    }

    val dependencyTree = DependencyTree(ide)
    with(dependencyTree.getTransitiveDependencies(plugin)) {
      assertEquals(2, size)
      // optional v1 dependency is not in the IDE as a bundled plugin
      assertNotContains("com.intellij.modules.idea.community")
      // optional v2 dependency
      assertContains("com.intellij.modules.lang")
      // dependency declared in a content module
      assertContains("com.intellij.modules.structuralsearch")
    }
  }

  private fun buildCorrectPlugin(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationSuccess<IdePlugin> {
    val pluginCreationResult = buildIdePlugin(pluginContentBuilder)
    if (pluginCreationResult !is PluginCreationSuccess) {
      fail("This plugin has not been created. Creation failed with error(s).")
    }
    return pluginCreationResult as PluginCreationSuccess
  }

  private fun buildIdePlugin(pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath(), pluginContentBuilder)
    return IdePluginManager.createManager().createPlugin(pluginFile, validateDescriptor = true, problemResolver = pluginProblemRemapper)
  }

  @Language("XML")
  val pluginXmlContent = """
      <idea-plugin>
          <id>com.intellij.java</id>
          <vendor>JetBrains</vendor>
          <dependencies>
              <plugin id="com.intellij.modules.lang"/>
          </dependencies>
          <module value="com.intellij.modules.java"/>
          <depends optional="true" config-file="community-integration.xml">com.intellij.modules.idea.community</depends>
          <content>
              <module name="intellij.java.structuralSearch"><![CDATA[<idea-plugin>
                <dependencies>
                  <plugin id="com.intellij.modules.structuralsearch" />
                </dependencies>
                <extensions defaultExtensionNs="com.intellij">
                  <applicationService serviceInterface="com.intellij.structuralsearch.plugin.ui.StructuralSearchTemplateBuilder" serviceImplementation="com.intellij.structuralsearch.java.ui.JavaStructuralSearchTemplateBuilder" overrides="true" />
                  <structuralsearch.profile implementation="com.intellij.structuralsearch.JavaStructuralSearchProfile" />
                  <java.elementFinder implementation="com.intellij.structuralsearch.IdeaOpenApiClassFinder" />
                </extensions>
              </idea-plugin>]]>
              </module>
          </content>
      </idea-plugin>
    """.trimIndent()

  @After
  fun tearDown() {
    this.pluginProblemRemapper.reset()
  }

  private fun Set<Dependency>.assertContains(id: String): Boolean =
    filterIsInstance<PluginAware>()
      .any { it.plugin.pluginId == id }

  private fun Set<Dependency>.assertNotContains(id: String): Boolean =
    filterIsInstance<PluginAware>()
      .none { it.plugin.pluginId == id }

  private fun List<PluginDependency>.assertContains(id: String): Boolean =
    any { it.id == id }

}