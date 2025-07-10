package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.closeAll
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.PluginArchiveManager
import com.jetbrains.plugin.structure.intellij.plugin.createIdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.JetBrainsPluginCreationResultResolver
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert.assertEquals
import java.io.Closeable
import java.nio.file.Path

abstract class IdePluginManagerTest(fileSystemType: FileSystemType) : BasePluginManagerTest<IdePlugin, IdePluginManager>(fileSystemType), Closeable {
  private val closeables = mutableListOf<Closeable>()

  override fun createManager(extractDirectory: Path): IdePluginManager {
    val archiveManager = createArchiveManager(extractDirectory)
    closeables += archiveManager
    return createIdePluginManager(archiveManager)
  }

  protected fun buildPluginSuccess(expectedWarnings: List<PluginProblem>, pluginFileBuilder: () -> Path): IdePlugin {
    return buildPluginSuccess(expectedWarnings, ::pluginFactory, pluginFileBuilder)
  }

  protected fun buildPluginSuccess(expectedWarnings: List<PluginProblem>, pluginFactory: IdePluginFactory = ::defaultPluginFactory, pluginFileBuilder: () -> Path): IdePlugin {
    val pluginFile = pluginFileBuilder()
    val successResult = createPluginSuccessfully(pluginFile, pluginFactory)
    val (plugin, warnings) = successResult
    assertEquals(expectedWarnings.toSet().sortedBy { it.message }, warnings.toSet().sortedBy { it.message })
    assertEquals(pluginFile, plugin.originalFile)
    return plugin
  }

  protected fun pluginFactory(pluginManager: IdePluginManager, pluginArtifactPath: Path): PluginCreationResult<IdePlugin> {
    val problemResolver = JetBrainsPluginCreationResultResolver.fromClassPathJson(IntelliJPluginCreationResultResolver())
    return pluginManager.createPlugin(pluginArtifactPath, validateDescriptor = true, problemResolver = problemResolver)
  }

  protected fun createArchiveManager(extractDirectory: Path): PluginArchiveManager {
    return PluginArchiveManager(extractDirectory)
  }

  override fun close() {
    closeables.closeAll()
  }
}