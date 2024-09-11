package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

abstract class BasePluginTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  protected fun buildPluginWithResult(problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver(),
                                      pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(pluginJarPath, pluginContentBuilder)
    val ideManager = IdePluginManager.createManager()
    return ideManager.createPlugin(pluginFile, validateDescriptor = true, problemResolver = problemResolver)
  }

  protected val pluginJarPath: Path
    get() = temporaryFolder.newFile("plugin.jar").toPath()

  protected val ideaPath: Path
    get() = temporaryFolder.newFolder("idea").toPath()

  protected fun assertEmpty(collectionDescription: String, collection: Collection<*>) {
    if (collection.isNotEmpty()) {
      fail("$collectionDescription not empty (${collection.size} elements): $collection")
    }
  }
}