package com.jetbrains.pluginverifier.tests

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.isInstance
import com.jetbrains.plugin.structure.base.utils.contentBuilder.ContentBuilder
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.plugin.IdePlugin
import com.jetbrains.plugin.structure.intellij.plugin.IdePluginManager
import com.jetbrains.plugin.structure.intellij.plugin.StructurallyValidated
import com.jetbrains.plugin.structure.intellij.problems.IntelliJPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.PluginCreationResultResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.reflect.KClass
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

  protected fun assertMatchingPluginProblems(pluginResult: PluginCreationSuccess<IdePlugin>) {
    with(pluginResult) {
      if (plugin is StructurallyValidated) {
        val plugin = plugin as StructurallyValidated
        val resultProblems = warnings + unacceptableWarnings
        assertEquals(resultProblems, plugin.problems)
      }
    }
  }

  @Throws(AssertionError::class)
  protected fun assertSuccess(
    pluginResult: PluginCreationResult<IdePlugin>,
    successHandler: PluginCreationSuccess<IdePlugin>.() -> Unit = {}
  ) {
    when (pluginResult) {
      is PluginCreationSuccess -> return successHandler(pluginResult)
      is PluginCreationFail -> with(pluginResult.errorsAndWarnings) {
        fail("Expected successful plugin creation, but got $size problem(s): "
          + joinToString { it.message })
      }
    }
    throw AssertionError("Expected success but got failure")
  }

  protected inline fun <reified T : PluginProblem> PluginCreationResult<IdePlugin>.assertContains(message: String) {
    val problems = when (this) {
      is PluginCreationSuccess -> warnings + unacceptableWarnings
      is PluginCreationFail -> errorsAndWarnings
    }
    assertContains(problems, T::class, message)
  }

  @Throws(AssertionError::class)
  protected fun assertNoProblems(problems: Collection<PluginProblem>) {
    with(problems) {
      if (!isEmpty()) {
        fail("Expected no problems, but got $size problem(s): " + joinToString { it.message })
      }
    }
  }

  protected fun assertContains(
    pluginProblems: Collection<PluginProblem>,
    pluginProblemClass: KClass<out PluginProblem>,
    message: String
  ) {
    val problems = pluginProblems.filter { problem ->
      problem.isInstance(pluginProblemClass)
    }
    if (problems.isEmpty()) {
      fail("Plugin creation result does not contain any problem of class [${pluginProblemClass.qualifiedName}]")
      return
    }
    val problemsWithMessage = problems.filter { it.message == message }
    if (problemsWithMessage.isEmpty()) {
      fail("Plugin creation result has ${problems.size} problem of class [${pluginProblemClass.qualifiedName}], " +
        "but none has a message '$message'. " +
        "Found [" + problems.joinToString { it.message } + "]"
      )
      return
    }
  }
}