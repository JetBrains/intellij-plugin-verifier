package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.ide.Ide
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.dependencies.graph.DepEdge
import com.jetbrains.pluginverifier.dependencies.graph.DepGraph2ApiGraphConverter
import com.jetbrains.pluginverifier.dependencies.graph.DepGraphBuilder
import com.jetbrains.pluginverifier.dependencies.graph.DepVertex
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.IdeDependencyFinder
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.PluginDetailsProviderImpl
import com.jetbrains.pluginverifier.repository.PluginFilesBank
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.cleanup.IdleSweepPolicy
import com.jetbrains.pluginverifier.repository.files.FileRepository
import com.jetbrains.pluginverifier.repository.provider.ProvideResult
import com.jetbrains.pluginverifier.repository.provider.ResourceProvider
import com.jetbrains.pluginverifier.tests.mocks.MockIde
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.MockPluginRepositoryAdapter
import com.jetbrains.pluginverifier.tests.mocks.createMockIdeaCorePlugin
import org.jgrapht.graph.DefaultDirectedGraph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class IdeDependencyFinderTest {

  @JvmField
  @Rule
  var tempFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun `get all plugin transitive dependencies`() {
    /*
    Given following dependencies between plugins:

    `test` -> `someModule` (defined in `moduleContainer`)
    `test` -> `somePlugin`

    `myPlugin` -> `test`
    `myPlugin` -> `externalModule` (defined in external plugin `externalPlugin` which is impossible to download)
    `myPlugin` -> `com.intellij.modules.platform` (default module)

    Should find dependencies on `test`, `somePlugin`,
    `moduleContainer` and `com.intellij` (which is the 'IDEA CORE' plugin).

    Dependency on `com.intellij.modules.platform` must not be indicated.
    Dependency resolution on `externalPlugin` must fail.
     */
    val testPlugin = MockIdePlugin(
        pluginId = "test",
        pluginVersion = "1.0",
        dependencies = listOf(PluginDependencyImpl("someModule", false, true), PluginDependencyImpl("somePlugin", false, false))
    )
    val somePlugin = MockIdePlugin(
        pluginId = "somePlugin",
        pluginVersion = "1.0"
    )
    val moduleContainer = MockIdePlugin(
        pluginId = "moduleContainer",
        pluginVersion = "1.0",
        definedModules = setOf("someModule")
    )

    val ide = MockIde(
        IdeVersion.createIdeVersion("IU-144"),
        bundledPlugins = listOf(
            createMockIdeaCorePlugin(tempFolder.newFolder("idea.core")),
            testPlugin,
            somePlugin,
            moduleContainer
        )
    )

    val externalModuleDependency = PluginDependencyImpl("externalModule", false, true)
    val startPlugin = MockIdePlugin(
        pluginId = "myPlugin",
        pluginVersion = "1.0",
        dependencies = listOf(
            PluginDependencyImpl("test", true, false),
            externalModuleDependency,
            PluginDependencyImpl("com.intellij.modules.platform", false, true)
        )
    )

    val ideDependencyFinder = configureTestIdeDependencyFinder(ide)

    val start = DepVertex("myPlugin", DependencyFinder.Result.FoundPlugin(startPlugin))
    val graph = DefaultDirectedGraph<DepVertex, DepEdge>(DepEdge::class.java)
    val depGraphBuilder = DepGraphBuilder(ideDependencyFinder)
    depGraphBuilder.buildDependenciesGraph(graph, start)

    val dependenciesGraph = DepGraph2ApiGraphConverter(IdeVersion.createIdeVersion("IU-181.1")).convert(graph, start)
    val deps = dependenciesGraph.vertices.map { it.pluginId }
    assertEquals(setOf("myPlugin", "test", "moduleContainer", "somePlugin", "com.intellij"), deps.toSet())

    assertEquals(listOf(MissingDependency(externalModuleDependency, "Failed to download test.")), dependenciesGraph.verifiedPlugin.missingDependencies)
    assertTrue(dependenciesGraph.getMissingDependencyPaths().size == 1)
  }

  private fun configureTestIdeDependencyFinder(ide: Ide): IdeDependencyFinder {
    val pluginRepository = object : MockPluginRepositoryAdapter() {
      override fun getIdOfPluginDeclaringModule(moduleId: String) =
          if (moduleId == "externalModule") "externalPlugin" else null

      override fun getLastCompatibleVersionOfPlugin(ideVersion: IdeVersion, pluginId: String) =
          if (pluginId == "externalPlugin") createMockPluginInfo(pluginId, pluginId, "1.0") else null
    }

    val downloadProvider = object : ResourceProvider<PluginInfo, Path> {
      override fun provide(key: PluginInfo): ProvideResult<Path> {
        if (key.pluginId == "externalPlugin") {
          return ProvideResult.Failed("Failed to download test.", Exception())
        }
        val tempDir = createTempDir().apply { deleteOnExit() }.toPath()
        return ProvideResult.Provided(tempDir)
      }
    }

    val fileRepository = FileRepository(IdleSweepPolicy(), downloadProvider)

    val pluginFilesBank = PluginFilesBank(fileRepository)
    val pluginDetailsProvider = PluginDetailsProviderImpl(tempFolder.newFolder().toPath())
    val pluginDetailsCache = PluginDetailsCache(10, pluginDetailsProvider, pluginFilesBank)
    return IdeDependencyFinder(ide, pluginRepository, pluginDetailsCache)
  }

}
