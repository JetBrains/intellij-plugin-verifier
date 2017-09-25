package com.jetbrains.pluginverifier.tests.dependencies

import com.jetbrains.plugin.structure.intellij.classes.locator.ClassLocationsContainer
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.pluginverifier.dependencies.DepGraph2ApiGraphConverter
import com.jetbrains.pluginverifier.dependencies.DepGraphBuilder
import com.jetbrains.pluginverifier.dependencies.IdeDependencyResolver
import com.jetbrains.pluginverifier.dependencies.MissingDependency
import com.jetbrains.pluginverifier.plugin.PluginCreatorImpl
import com.jetbrains.pluginverifier.repository.DownloadPluginResult
import com.jetbrains.pluginverifier.repository.UpdateInfo
import com.jetbrains.pluginverifier.tests.mocks.MockIde
import com.jetbrains.pluginverifier.tests.mocks.MockIdePlugin
import com.jetbrains.pluginverifier.tests.mocks.MockPluginRepositoryAdapter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.Closeable
import java.io.File

/**
 * @author Sergey Patrikeev
 */
class IdeDependencyResolverTest {

  @Test
  fun `get all plugin transitive dependencies`() {
    /*
    Given following dependencies between plugins:

    `test` -> `someModule` (defined in `moduleContainer`)
    `test` -> `somePlugin`

    `myPlugin` -> `test`
    `myPlugin` -> `externalModule` (defined in external plugin `externalPlugin` which is impossible to download)

    Should find dependencies on `test`, `somePlugin` and `moduleContainer`.
    Dependency resolution on `externalPlugin` must fail.
     */
    val testPlugin = MockIdePlugin(pluginId = "test", pluginVersion = "1.0", dependencies = listOf(PluginDependencyImpl("someModule", false, true), PluginDependencyImpl("somePlugin", false, false)))
    val somePlugin = MockIdePlugin(pluginId = "somePlugin", pluginVersion = "1.0")
    val moduleContainer = MockIdePlugin(pluginId = "moduleContainer", pluginVersion = "1.0", definedModules = setOf("someModule"))

    val ide = MockIde(IdeVersion.createIdeVersion("IU-144"), bundledPlugins = listOf(testPlugin, somePlugin, moduleContainer))

    val externalModuleDependency = PluginDependencyImpl("externalModule", false, true)
    val plugin = MockIdePlugin(pluginId = "myPlugin", pluginVersion = "1.0", dependencies = listOf(PluginDependencyImpl("test", true, false), externalModuleDependency))

    val repository = object : MockPluginRepositoryAdapter() {
      override fun getIdOfPluginDeclaringModule(moduleId: String): String? {
        return if (moduleId == "externalModule") "externalPlugin" else null
      }

      override fun getLastCompatibleUpdateOfPlugin(ideVersion: IdeVersion, pluginId: String): UpdateInfo? {
        return if (pluginId == "externalPlugin") UpdateInfo(pluginId, pluginId, "1.0", 0, null) else null
      }

      override fun downloadPluginFile(update: UpdateInfo): DownloadPluginResult {
        return DownloadPluginResult.FailedToDownload(update, "Failed to download test.")
      }
    }

    val pluginCreator = PluginCreatorImpl(repository, File("isn't necessary"))
    val dependencyResolver = IdeDependencyResolver(ide, repository, pluginCreator)
    val (graph, start) = DepGraphBuilder(dependencyResolver).build(plugin, ClassLocationsContainer(Closeable { }, emptyMap()))
    val dependenciesGraph = DepGraph2ApiGraphConverter.convert(graph, start)

    val deps: List<String> = dependenciesGraph.vertices.map { it.id }
    assertEquals(setOf("myPlugin", "test", "somePlugin", "moduleContainer"), deps.toSet())

    assertEquals(listOf(MissingDependency(externalModuleDependency, "Failed to download test.")), dependenciesGraph.start.missingDependencies)
  }

}
