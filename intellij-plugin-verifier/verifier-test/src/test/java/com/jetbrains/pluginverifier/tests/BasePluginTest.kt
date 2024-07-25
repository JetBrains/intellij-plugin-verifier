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

  protected fun buildPluginWithResult(
    problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver(),
    pluginContentBuilder: ContentBuilder.() -> Unit
  ) = buildPluginWithResult(problemResolver, "plugin.jar", pluginContentBuilder)

  protected fun buildPluginWithResult(problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver(),
                                      pluginJarName: String,
                                      pluginContentBuilder: ContentBuilder.() -> Unit
  ): PluginCreationResult<IdePlugin> =
    buildPluginWithResult(
      problemResolver,
      { IdePluginManager.createManager() },
      pluginJarName,
      pluginContentBuilder = pluginContentBuilder
    )

  protected fun buildPluginWithResult(problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver(),
                                      pluginManagerProvider: () -> IdePluginManager,
                                      pluginJarName: String,
                                      pluginContentBuilder: ContentBuilder.() -> Unit
                                      ): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(temporaryFolder.newFile(pluginJarName).toPath(), pluginContentBuilder)
    return pluginManagerProvider().createPlugin(pluginFile, validateDescriptor = true, problemResolver = problemResolver)
  }
}