package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import org.junit.Rule
import org.junit.rules.TemporaryFolder

abstract class BasePluginTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  protected fun buildPluginWithResult(problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver(),
                                      pluginContentBuilder: ContentBuilder.() -> Unit
  ): PluginCreationResult<IdePlugin> =
    buildPluginWithResult(
      problemResolver,
      { IdePluginManager.createManager() },
      pluginContentBuilder = pluginContentBuilder
    )

  private fun buildPluginWithResult(problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver(),
                                    pluginManagerProvider: () -> IdePluginManager,
                                    pluginContentBuilder: ContentBuilder.() -> Unit
                                      ): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.jar").toPath(), pluginContentBuilder)
    return pluginManagerProvider().createPlugin(pluginFile, validateDescriptor = true, problemResolver = problemResolver)
  }
}