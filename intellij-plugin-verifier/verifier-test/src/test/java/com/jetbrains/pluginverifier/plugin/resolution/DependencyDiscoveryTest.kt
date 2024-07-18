package com.jetbrains.pluginverifier.plugin.resolution

import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.problems.NoModuleDependencies
import com.jetbrains.plugin.structure.intellij.problems.ReleaseDateInFuture
import com.jetbrains.plugin.structure.jar.META_INF
import com.jetbrains.plugin.structure.jar.PLUGIN_XML
import com.jetbrains.pluginverifier.dependencies.resolution.DependencyFinder
import com.jetbrains.pluginverifier.dependencies.resolution.LastVersionSelector
import com.jetbrains.pluginverifier.dependencies.resolution.RepositoryDependencyFinder
import com.jetbrains.pluginverifier.plugin.DefaultPluginDetailsProvider
import com.jetbrains.pluginverifier.plugin.PluginDetailsCache
import com.jetbrains.pluginverifier.plugin.SizeLimitedPluginDetailsCache
import com.jetbrains.pluginverifier.repository.PluginInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class DependencyDiscoveryTest {

  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  @Test
  fun `plugin dependencies are resolved`() {
    val pluginV1 = PluginInfo("somePlugin", "Some Plugin", "1.0.0")
    val pluginV2 = PluginInfo("somePlugin", "Some Plugin", "2.0.0")
    val dependency = PluginInfo("aDependency", "Dependency", "1.0.0")

    val pluginV1JarPath = temporaryFolder.createJarPath(pluginV1).also {
      buildPluginWithDescriptor(it) {
        pluginV1.getPluginXml(v1Dependencies = listOf("aDependency"))
      }
    }

    val pluginV2JarPath = temporaryFolder.createJarPath(pluginV2).also {
      buildPluginWithDescriptor(it) {
        pluginV2.getPluginXml(v1Dependencies = listOf("aDependency")) {
          append("""
            <product-descriptor 
              code="MOCKPLUGIN" 
              release-version="1" 
              release-date="99991212" />
          """.trimIndent())
        }
      }
    }

    val dependencyPath = temporaryFolder.createJarPath(dependency).also {
      buildPluginWithDescriptor(it) {
        dependency.getPluginXml()
      }
    }

    val repository = InMemoryPluginRepository.create(pluginV1, pluginV2)
    val versionSelector = LastVersionSelector()
    val fileProvider = InMemoryPluginFileProvider().apply {
      this[pluginV1] = pluginV1JarPath
      this[pluginV2] = pluginV2JarPath
      this[dependency] = dependencyPath
    }
    val detailsProvider = DefaultPluginDetailsProvider(temporaryFolder.newFolder("plugin-cache").toPath())
    val detailsCache = SizeLimitedPluginDetailsCache(Int.MAX_VALUE, fileProvider, detailsProvider)

    val dependencyFinder = RepositoryDependencyFinder(repository, versionSelector, detailsCache)

    val result = dependencyFinder.findPluginDependency("somePlugin", isModule = false)
    assertTrue(result is DependencyFinder.Result.DetailsProvided)
    val detailsProvided = result as DependencyFinder.Result.DetailsProvided
    assertTrue(detailsProvided.pluginDetailsCacheResult is PluginDetailsCache.Result.Provided)
    val cacheResult = detailsProvided.pluginDetailsCacheResult as PluginDetailsCache.Result.Provided
    with(cacheResult.pluginDetails) {
      with(pluginWarnings) {
        assertEquals(2, size)
        assertTrue(hasUnwrappedProblem<NoModuleDependencies>())
        assertTrue(hasUnwrappedProblem<ReleaseDateInFuture>())
      }
      with(idePlugin.dependencies) {
        assertEquals(1, size)
        assertEquals("aDependency", first().id)
      }
    }
  }

  private fun buildPluginWithDescriptor(pluginArtifactPath: Path, pluginXmlContent: ContentBuilder.() -> String): Path =
    buildZipFile(pluginArtifactPath) {
      dir(META_INF) {
        file(PLUGIN_XML) {
          pluginXmlContent()
        }
      }
    }

  private fun PluginInfo.getPluginXml(
    v1Dependencies: List<String> = emptyList(),
    additionalContents: StringBuilder.() -> Unit = {}
  ): String {
    val v1DependenciesString = v1Dependencies.joinToString("\n") { "<depends>$it</depends>" }
    val additionalString = StringBuilder().apply {
      additionalContents()
    }
    return """
        <idea-plugin>
          <id>$pluginId</id>
          <name>$pluginName</name>
          <version>$version</version>
          <vendor email="vendor@vendor.com" url="url">$vendor</vendor>
          $v1DependenciesString
          $additionalString
        </idea-plugin>
        """.trimIndent()
  }
}