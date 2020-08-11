package com.jetbrains.plugin.structure.ktor.mock

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.utils.contentBuilder.buildZipFile
import com.jetbrains.plugin.structure.base.utils.simpleName
import com.jetbrains.plugin.structure.ktor.KtorFeature
import com.jetbrains.plugin.structure.ktor.KtorFeaturePluginManager
import com.jetbrains.plugin.structure.ktor.bean.KtorVendor
import com.jetbrains.plugin.structure.ktor.bean.NAME
import com.jetbrains.plugin.structure.ktor.bean.VENDOR
import com.jetbrains.plugin.structure.ktor.problems.createIncorrectKtorFeatureFile
import com.jetbrains.plugin.structure.mocks.BasePluginManagerTest
import com.jetbrains.plugin.structure.rules.FileSystemType
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

class KtorInvalidPluginsTest(fileSystemType: FileSystemType) : BasePluginManagerTest<KtorFeature, KtorFeaturePluginManager>(fileSystemType) {
  override fun createManager(extractDirectory: Path): KtorFeaturePluginManager =
    KtorFeaturePluginManager.createManager(extractDirectory)

  @Test(expected = IllegalArgumentException::class)
  fun `file does not exist`() {
    assertProblematicPlugin(Paths.get("does-not-exist.zip"), emptyList())
  }

  @Test
  fun `invalid file extension`() {
    val incorrect = temporaryFolder.newFile("incorrect.json")
    assertProblematicPlugin(incorrect, listOf(createIncorrectKtorFeatureFile(incorrect.simpleName)))
  }

  @Test
  fun `name is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified(NAME)) { name = null }
    checkInvalidPlugin(PropertyNotSpecified(NAME)) { name = "" }
    checkInvalidPlugin(PropertyNotSpecified(NAME)) { name = "\n" }
  }

  @Test
  fun `vendor is not specified`() {
    checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = null }
    checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = KtorVendor() }
    checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = KtorVendor("") }
    checkInvalidPlugin(PropertyNotSpecified(VENDOR)) { vendor = KtorVendor("\n") }
  }

  private fun checkInvalidPlugin(problem: PluginProblem, descriptor: KtorPluginJsonBuilder.() -> Unit) {
    val pluginFile = buildZipFile(temporaryFolder.newFolder().resolve("feature.zip")) {
      file(KtorFeaturePluginManager.DESCRIPTOR_NAME) {
        val builder = perfectEduPluginBuilder
        builder.descriptor()
        builder.asString()
      }
    }
    assertProblematicPlugin(pluginFile, listOf(problem))
  }
}