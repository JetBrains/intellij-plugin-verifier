package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginCreationResult
import com.jetbrains.plugin.structure.base.plugin.PluginCreationSuccess
import com.jetbrains.plugin.structure.base.problems.PluginDescriptorIsNotFound
import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.intellij.problems.DuplicatedDependencyWarning
import com.jetbrains.plugin.structure.intellij.problems.OptionalDependencyDescriptorResolutionProblem
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class PluginIconsTest(fileSystemType: FileSystemType) : BasePluginManagerTest<IdePlugin, IdePluginManager>(fileSystemType) {
  private val mockPluginRoot: Path = this::class.java.getResource("/mock-plugin").let {
    if (it === null) {
      throw IllegalStateException("Cannot load resource for /mock-plugin")
    }
    Paths.get(it.toURI())
  }

  private val metaInfDir = mockPluginRoot.resolve("META-INF")

  private val optionalsDir = mockPluginRoot.resolve("optionalsDir")
  private val propertiesDir = mockPluginRoot.resolve("properties")
  private val somePackageDir = mockPluginRoot.resolve("classes").resolve("somePackage")

  private val compileLibraryDir = mockPluginRoot.resolve("compileLibrary")

  private val expectedWarnings = listOf(
    OptionalDependencyDescriptorResolutionProblem(
      "missingDependency",
      "missingFile.xml",
      listOf(PluginDescriptorIsNotFound("missingFile.xml"))
    ),
    OptionalDependencyDescriptorResolutionProblem(
      "referenceFromRoot",
      "/META-INF/referencedFromRoot.xml",
      listOf(PluginDescriptorIsNotFound("/META-INF/referencedFromRoot.xml"))
    ),
    DuplicatedDependencyWarning("duplicatedDependencyId"),
  )

  override fun createManager(extractDirectory: Path) = IdePluginManager.createManager(extractDirectory)

  @Test
  fun `icons are loaded`() {
    val plugin = buildPluginSuccess(expectedWarnings) {
      buildZipFile(temporaryFolder.newFile("plugin.jar")) {
        dir("META-INF", metaInfDir)
        dir("optionalsDir", optionalsDir)
        dir("somePackage", somePackageDir)
        dir("properties", propertiesDir)
      }
    }
    assertEquals(1, plugin.icons.size)
  }

  @Test
  fun `icon loading is skipped`() {
    val doNotLoadIcons = PluginParsingConfiguration(loadIcons = false)
    val pluginFactory = PluginFactory { pluginManager, pluginArtifactPath ->
      pluginManager.createPlugin(pluginArtifactPath, parsingConfiguration = doNotLoadIcons)
    }
    val plugin = buildPluginSuccess(expectedWarnings, pluginFactory) {
      buildZipFile(temporaryFolder.newFile("plugin.jar")) {
        dir("META-INF", metaInfDir)
        dir("optionalsDir", optionalsDir)
        dir("somePackage", somePackageDir)
        dir("properties", propertiesDir)
      }
    }
    assertEquals(0, plugin.icons.size)
  }

  private fun buildPluginSuccess(expectedWarnings: List<PluginProblem>, pluginFactory: PluginFactory? = null, pluginFileBuilder: () -> Path): IdePlugin {
    val pluginFile = pluginFileBuilder()

    val aPluginFactory = pluginFactory ?: DefaultPluginFactory
    val successResult = doCreatePluginSuccessfully(pluginFile, aPluginFactory)
    val (plugin, warnings) = successResult
    assertEquals(expectedWarnings.toSet().sortedBy { it.message }, warnings.toSet().sortedBy { it.message })
    assertEquals(pluginFile, plugin.originalFile)
    return plugin
  }

  fun doCreatePluginSuccessfully(pluginArtifactPath: Path, pluginFactory: PluginFactory): PluginCreationSuccess<IdePlugin> {
    val pluginManager = createManager(temporaryFolder.newFolder("extract"))
    val pluginCreationResult = pluginFactory.create(pluginManager, pluginArtifactPath)
    if (pluginCreationResult is PluginCreationFail) {
      Assert.fail(pluginCreationResult.errorsAndWarnings.joinToString())
    }
    return pluginCreationResult as PluginCreationSuccess<IdePlugin>
  }
}

fun interface PluginFactory {
  fun create(idePluginManager: IdePluginManager, pluginArtifactPath: Path): PluginCreationResult<IdePlugin>
}

object DefaultPluginFactory: PluginFactory {
  override fun create(idePluginManager: IdePluginManager, pluginArtifactPath: Path): PluginCreationResult<IdePlugin> {
    return idePluginManager.createPlugin(pluginArtifactPath)
  }
}
