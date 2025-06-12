package com.jetbrains.pluginverifier.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.intellij.classes.locator.CompileServerExtensionKey
import com.jetbrains.plugin.structure.intellij.classes.plugin.BundledPluginClassesFinder
import com.jetbrains.plugin.structure.intellij.classes.plugin.IdePluginClassesLocations
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.JetBrainsPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import com.jetbrains.pluginverifier.repository.PluginInfo
import com.jetbrains.pluginverifier.repository.files.FileLock
import com.jetbrains.pluginverifier.repository.repositories.bundled.BundledPluginInfo
import com.jetbrains.pluginverifier.repository.repositories.dependency.DependencyPluginInfo
import java.nio.file.Path

/**
 * Provides plugin details with explicit support for bundled plugins that are provided by the Platform
 * or downloaded as a dependency from JetBrains Marketplace.
 *
 * Non-bundled plugins that aren't dependencies are handled by the delegate [PluginDetailsProviderImpl].
 */
class DefaultPluginDetailsProvider(extractDirectory: Path) : AbstractPluginDetailsProvider(extractDirectory), AutoCloseable {
  private val nonBundledPluginDetailsProvider: PluginDetailsProviderImpl = PluginDetailsProviderImpl(extractDirectory)

  private val dependencyProblemResolver: PluginCreationResultResolver =
    JetBrainsPluginCreationResultResolver.fromClassPathJson(IntelliJPluginCreationResultResolver())

  private val closeableResources: MutableList<AutoCloseable> = mutableListOf()

  override fun readPluginClasses(pluginInfo: PluginInfo, idePlugin: IdePlugin): IdePluginClassesLocations {
    return if (pluginInfo is BundledPluginInfo) {
      BundledPluginClassesFinder.findPluginClasses(idePlugin, additionalKeys = listOf(CompileServerExtensionKey))
    } else {
      nonBundledPluginDetailsProvider.readPluginClasses(pluginInfo, idePlugin)
    }
  }

  override fun createPlugin(pluginInfo: PluginInfo, pluginFileLock: FileLock): PluginCreationResult<IdePlugin> {
    return if (pluginInfo is DependencyPluginInfo) {
      idePluginManager
        .createPlugin(
          pluginFileLock.file,
          validateDescriptor = false,
          problemResolver = dependencyProblemResolver,
          deleteExtractedDirectory = false
        ).registerCloseableResources()
    } else {
      super.createPlugin(pluginInfo, pluginFileLock)
    }
  }

  private fun PluginCreationResult<IdePlugin>.registerCloseableResources() = apply {
    if (this is PluginCreationSuccess) {
      closeableResources += resources
    }
  }

  override fun close() {
    closeableResources.forEach { it.close() }
  }
}