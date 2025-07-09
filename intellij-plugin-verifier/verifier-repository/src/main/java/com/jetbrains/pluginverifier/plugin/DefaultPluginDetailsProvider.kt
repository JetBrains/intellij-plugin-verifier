package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.BundledPluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.ClassSearchContext
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.JetBrainsPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.PluginArchiveResource
import com.jetbrains.plugin.structure.intellij.utils.DeletableOnClose
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.dependency.DependencyPluginInfo
import java.io.Closeable
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides plugin details with explicit support for bundled plugins that are provided by the Platform
 * or downloaded as a dependency from JetBrains Marketplace.
 *
 * Non-bundled plugins that aren't dependencies are handled by the delegate [PluginDetailsProviderImpl].
 */
class DefaultPluginDetailsProvider(
  archiveManager: PluginArchiveManager
) : AbstractPluginDetailsProvider(archiveManager), AutoCloseable {

  private val nonBundledPluginDetailsProvider: PluginDetailsProviderImpl = PluginDetailsProviderImpl(archiveManager)

  private val dependencyDetailsProvider = DependencyDetailsProvider(archiveManager)

  private val dependencyProblemResolver: PluginCreationResultResolver =
    JetBrainsPluginCreationResultResolver.fromClassPathJson(IntelliJPluginCreationResultResolver())

  private val extractedPluginLocationCache = ConcurrentHashMap<Path, PluginCreationResult<IdePlugin>>()

  private val closeableResources: MutableList<Closeable> = mutableListOf()

  override fun readPluginClasses(pluginInfo: PluginInfo, idePlugin: IdePlugin): IdePluginClassesLocations {
    return when (pluginInfo) {
      is BundledPluginInfo ->
        BundledPluginClassesFinder.findPluginClasses(idePlugin, additionalKeys = listOf(CompileServerExtensionKey),
          ClassSearchContext(archiveManager))

      is DependencyPluginInfo ->
        dependencyDetailsProvider.readPluginClasses(pluginInfo, idePlugin)

      else -> nonBundledPluginDetailsProvider.readPluginClasses(pluginInfo, idePlugin)
    }
  }

  override fun createPlugin(pluginInfo: PluginInfo, pluginFileLock: FileLock): PluginCreationResult<IdePlugin> {
    return if (pluginInfo is DependencyPluginInfo) {
      synchronized(extractedPluginLocationCache) {
        val pluginArtifactPath = pluginFileLock.file
        if (extractedPluginLocationCache.containsKey(pluginArtifactPath)) {
          eventLog.logCached(pluginArtifactPath)
          extractedPluginLocationCache.getValue(pluginArtifactPath)
        } else {
          idePluginManager
            .createPlugin(
              pluginArtifactPath,
              validateDescriptor = false,
              problemResolver = dependencyProblemResolver,
              deleteExtractedDirectory = false
            ).also {
              eventLog.logExtracted(pluginArtifactPath)
              it.registerCloseableResources()
                .cacheExtractedDirectory(pluginArtifactPath)
            }
        }
      }
    } else {
      super.createPlugin(pluginInfo, pluginFileLock)
    }
  }

  private fun PluginCreationResult<IdePlugin>.registerCloseableResources() = apply {
    if (this is PluginCreationSuccess) {
      resources.forEach {
        if (it !is PluginArchiveResource) {
          closeableResources += DeletableOnClose.of(it)
        }
      }
    }
  }

  private fun PluginCreationResult<IdePlugin>.cacheExtractedDirectory(artifactPath: Path) = apply {
    if (this is PluginCreationSuccess) {
      extractedPluginLocationCache[artifactPath] = this
    }
  }

  override fun close() {
    archiveManager.delete()
    eventLog.clear()
    closeableResources.closeAll()
  }

  val closeableResourcesSize: Int
    get() = closeableResources.size

  val eventLog = EventLog()

  class EventLog() : AbstractList<String>() {
    private val events: MutableList<String> = mutableListOf()

    fun logCached(path: Path) = log(path, true)

    fun logExtracted(path: Path) = log(path, false)

    fun log(path: Path, isCached: Boolean) {
      events += (if (isCached) "cached " else "extracted ") + path
    }

    override val size: Int
      get() = events.size

    override fun get(index: Int): String = events[index]

    fun clear() = events.clear()
  }
}