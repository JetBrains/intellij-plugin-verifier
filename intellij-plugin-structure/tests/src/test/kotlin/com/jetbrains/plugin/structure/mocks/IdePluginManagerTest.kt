package com.jetbrains.plugin.structure.mocks

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.JetBrainsPluginCreationResultResolver
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert.assertEquals
import java.nio.file.Path

abstract class IdePluginManagerTest (fileSystemType: FileSystemType) : BasePluginManagerTest<IdePlugin, IdePluginManager>(fileSystemType) {
  override fun createManager(extractDirectory: Path): IdePluginManager =
    IdePluginManager.createManager(extractDirectory)

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
}