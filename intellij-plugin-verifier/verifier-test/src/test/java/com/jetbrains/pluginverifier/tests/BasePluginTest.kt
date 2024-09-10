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

  protected val pluginJarPath: Path
    get() = temporaryFolder.newFile("plugin.jar").toPath()

  protected val ideaPath: Path
    get() = temporaryFolder.newFolder("idea").toPath()

  protected fun buildPluginWithResult(problemResolver: PluginCreationResultResolver = IntelliJPluginCreationResultResolver(),
                                      pluginContentBuilder: ContentBuilder.() -> Unit): PluginCreationResult<IdePlugin> {
    val pluginFile = buildZipFile(pluginJarPath, pluginContentBuilder)
    val ideManager = IdePluginManager.createManager()
    return ideManager.createPlugin(pluginFile, validateDescriptor = true, problemResolver = problemResolver)
  }

  protected fun assertEmpty(collectionDescription: String, collection: Collection<*>) {
    if (collection.isNotEmpty()) {
      fail("$collectionDescription not empty (${collection.size} elements): $collection")
    }
  }

  protected fun ideaPlugin(pluginId: String = "someid",
                           pluginName: String = "someName",
                           pluginVersion: String = "1",
                           vendor: String = "vendor",
                           sinceBuild: String = "131.1",
                           untilBuild: String = "231.1",
                           description: String = "this description is looooooooooong enough"): String = """
    <id>$pluginId</id>
    <name>$pluginName</name>
    <version>$pluginVersion</version>
    ""<vendor email="vendor.com" url="url">$vendor</vendor>""
    <description>$description</description>
    <change-notes>these change-notes are looooooooooong enough</change-notes>
    <idea-version since-build="$sinceBuild" until-build="$untilBuild"/>
    <depends>com.intellij.modules.platform</depends>
  """
}