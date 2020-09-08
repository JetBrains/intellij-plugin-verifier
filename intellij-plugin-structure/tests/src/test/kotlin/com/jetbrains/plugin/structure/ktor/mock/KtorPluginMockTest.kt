package com.jetbrains.plugin.structure.ktor.mock

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.ktor.KtorFeature
import com.jetbrains.plugin.structure.ktor.KtorFeaturePluginManager
import com.jetbrains.plugin.structure.ktor.bean.CodeTemplate
import com.jetbrains.plugin.structure.ktor.bean.FeatureInstallRecipe
import com.jetbrains.plugin.structure.ktor.bean.KtorFeatureDescriptor
import com.jetbrains.plugin.structure.ktor.bean.Position
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test
import java.nio.file.Path

class KtorPluginMockTest(fileSystemType: FileSystemType) : BasePluginManagerTest<KtorFeature, KtorFeaturePluginManager>(fileSystemType) {

  private val iconTestContent = "<svg></svg>"

  override fun createManager(extractDirectory: Path): KtorFeaturePluginManager =
    KtorFeaturePluginManager.createManager(extractDirectory)

  @Test
  fun `parse base fields ktor plugin test`() {
    val pluginFile = buildZipFile(temporaryFolder.newFile("plugin.zip")) {
    file(KtorFeaturePluginManager.DESCRIPTOR_NAME) {
    getMockPluginJsonContent("descriptor")
    }
    file("featureIcon.svg", iconTestContent)
    }
    testMockPluginStructureAndConfiguration(pluginFile)
  }

  private fun testMockPluginStructureAndConfiguration(pluginFile: Path) {
    val pluginCreationSuccess = createPluginSuccessfully(pluginFile)

    testMockConfigs(pluginCreationSuccess.plugin)
    testMockWarnings(pluginCreationSuccess.warnings)
  }

  private fun testMockWarnings(problems: List<PluginProblem>) {
    Assert.assertTrue(problems.isEmpty())
  }

  private fun testMockConfigs(plugin: KtorFeature) {
    Assert.assertEquals("Configuration feature", plugin.pluginName)
    Assert.assertEquals("Amazing feature", plugin.description)
    Assert.assertEquals("JetBrains s.r.o.", plugin.vendor)
    Assert.assertEquals("ktor.feature.configuration", plugin.pluginId)
    Assert.assertEquals(iconTestContent, String(plugin.icons.single().content))

    Assert.assertNotNull(plugin.fullDescriptorJson)
    val deserializedDescriptor = Json.parse(
      KtorFeatureDescriptor.serializer(),
      plugin.fullDescriptorJson!!
    )

    Assert.assertEquals("Configuration feature", deserializedDescriptor.pluginName)
    Assert.assertEquals("Amazing feature", deserializedDescriptor.shortDescription)
    Assert.assertEquals("JetBrains s.r.o.", deserializedDescriptor.vendor)
    Assert.assertEquals("ktor.feature.configuration", deserializedDescriptor.pluginId)
    Assert.assertEquals(
      FeatureInstallRecipe(
        imports = listOf("io.ktor.configuration.*"),
        testImports = listOf("kotlin.test.*"),
        installBlock = "install(Configuration) {\n\n}",
        templates = listOf(
          CodeTemplate(
            position = Position.INSIDE_APPLICATION_MODULE,
            text = "val x = 0\nval y = x + 1"
          ),
          CodeTemplate(
            position = Position.TEST_FUNCTION,
            text = "@Test\nfun testF() {\n\tassertEquals(1, 1)\n}"
          )
        )
      ),
      deserializedDescriptor.installRecipe
    )
    Assert.assertNotNull(deserializedDescriptor.gradleInstall)
    Assert.assertNotNull(deserializedDescriptor.mavenInstall)
    Assert.assertTrue(deserializedDescriptor.gradleInstall!!.dependencies.isNotEmpty())
    Assert.assertEquals(
      deserializedDescriptor.gradleInstall!!.dependencies,
      deserializedDescriptor.mavenInstall!!.dependencies
    )
  }
}


