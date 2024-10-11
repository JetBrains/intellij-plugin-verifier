package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.base.utils.createParentDirs
import com.jetbrains.plugin.structure.base.utils.writeText
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipOutputStream

private const val IDEA_ULTIMATE_2024_2 = "IU-242.18071.24"

class ProductInfoClassResolverTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  @Before
  fun setUp() {
    with(temporaryFolder.newFolder("idea")) {
      ideRoot = toPath()
      val productInfoJsonPath = ideRoot.resolve("product-info.json")
      copyResource("/ide/productInfo/product-info_mini.json", productInfoJsonPath)

      ideRoot.resolve("build.txt").writeText(IDEA_ULTIMATE_2024_2)

      createEmptyIdeFiles()
    }
  }

  @Test
  fun `resolver is created from an IDE instance`() {
    val ide = MockIde(IdeVersion.createIdeVersion(IDEA_ULTIMATE_2024_2), ideRoot)
    val resolver = ProductInfoClassResolver.of(ide)
    with(resolver.layoutComponentResolverNames) {
      assertEquals(5, size)
      assertEquals(
        listOf(
          "Git4Idea",
          "com.intellij",
          "intellij.copyright.vcs",
          "intellij.execution.process.elevation",
          "intellij.java.featuresTrainer"
        ), this
      )
    }
  }

  @Test
  fun `resolver supports 242+ IDE`() {
    assertTrue(ProductInfoClassResolver.supports(ideRoot))
  }

  private fun copyResource(resource: String, targetFile: Path) {
    val url: URL = this::class.java.getResource(resource) ?: throw AssertionError("Resource '$resource' not found")
    url.openStream().use {
      Files.copy(it, targetFile)
    }
  }

  private fun createEmptyIdeFiles() {
    ideFiles.flatMap { (pluginId, files) -> files }
      .map { file ->
        ideRoot.resolve(file).apply {
          createParentDirs()
        }
      }.forEach {
        it.createEmptyZip()
      }
  }

  private fun Path.createEmptyZip() {
    ZipOutputStream(Files.newOutputStream(this)).use {}
  }


  private val ideFiles = mapOf<PluginId, List<String>>(
    "Git4Idea" to listOf(
      "plugins/vcs-git/lib/vcs-git.jar",
      "plugins/vcs-git/lib/git4idea-rt.jar"
    ),
    "com.intellij" to listOf(
      "lib/platform-loader.jar",
      "lib/util-8.jar",
      "lib/util.jar",
      "lib/util_rt.jar",
      "lib/product.jar",
      "lib/opentelemetry.jar",
      "lib/app.jar",
      "lib/stats.jar",
      "lib/jps-model.jar",
      "lib/external-system-rt.jar",
      "lib/rd.jar",
      "lib/bouncy-castle.jar",
      "lib/protobuf.jar",
      "lib/intellij-test-discovery.jar",
      "lib/forms_rt.jar",
      "lib/lib.jar",
      "lib/externalProcess-rt.jar",
      "lib/groovy.jar",
      "lib/annotations.jar",
      "lib/idea_rt.jar",
      "lib/intellij-coverage-agent-1.0.750.jar",
      "lib/jsch-agent.jar",
      "lib/junit.jar",
      "lib/junit4.jar",
      "lib/nio-fs.jar",
      "lib/testFramework.jar",
      "lib/trove.jar"
    ),
    "intellij.execution.process.elevation" to listOf(
      "lib/modules/intellij.execution.process.elevation.jar"
    ),
    "intellij.java.featuresTrainer" to listOf(
      "plugins/java/lib/modules/intellij.java.featuresTrainer.jar"
    )
  )
}

private typealias PluginId = String

