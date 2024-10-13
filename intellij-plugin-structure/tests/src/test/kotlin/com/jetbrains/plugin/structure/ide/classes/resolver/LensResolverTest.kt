package com.jetbrains.plugin.structure.ide.classes.resolver

import com.jetbrains.plugin.structure.base.utils.createParentDirs
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent.Plugin
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent.PluginAlias
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.plugin.PluginDependencyImpl
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import com.jetbrains.plugin.structure.mocks.MockIde
import com.jetbrains.plugin.structure.mocks.MockIdePlugin
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipOutputStream

class LensResolverTest {
  @Rule
  @JvmField
  val temporaryFolder = TemporaryFolder()

  private lateinit var ideRoot: Path

  @Test
  fun name() {
    ideRoot = temporaryFolder.newFolder("idea").toPath()

    val ideVersion = IdeVersion.createIdeVersion("IU-243.12818.47")

    val plugin = MockIdePlugin(
      dependencies = listOf(
        PluginDependencyImpl(/* id = */ "com.intellij.modules.platform",
          /* isOptional = */ false,
          /* isModule = */ true
        ),
        PluginDependencyImpl(/* id = */ "com.intellij.modules.json",
          /* isOptional = */ false,
          /* isModule = */ true
        ),
      )
    )

    val productInfo = ProductInfo(
      name = "IntelliJ IDEA",
      version = "2024.3",
      versionSuffix = "EAP",
      buildNumber = ideVersion.asStringWithoutProductCode(),
      productCode = "IU",
      dataDirectoryName = "IntelliJIdea2024.3",
      productVendor = "JetBrains",
      svgIconPath = "bin/idea.svg",
      modules = emptyList(),
      bundledPlugins = emptyList(),
      layout = listOf(
        PluginAlias("com.intellij.modules.platform"),
        Plugin("com.intellij.modules.json", listOf("plugins/json/lib/json.jar")),
        Plugin("Git4Idea", listOf("plugins/vcs-git/lib/vcs-git.jar", "plugins/vcs-git/lib/git4idea-rt.jar")),
      )
    )

    productInfo.createEmptyLayoutComponentPaths(ideRoot)

    val ide = MockIde(ideVersion, ideRoot)

    val productInfoClassResolver = ProductInfoClassResolver(productInfo, ide)
    val lensResolver = LensResolver(plugin, productInfoClassResolver)

    val x = lensResolver.filteredResolvers
    with(x) {
      assertEquals(1, size)
    }
  }

  private fun ProductInfo.createEmptyLayoutComponentPaths(ideRoot: Path) {
    layout
      .flatMap { if (it is LayoutComponent.Classpathable) it.getClasspath() else emptyList() }
      .map { ideRoot.resolve(it) }
      .map { it.mkdirs() }
      .forEach {
        it.createEmptyZip()
      }
  }

  private fun Path.mkdirs(): Path = apply {
    createParentDirs()
  }

  private fun Path.createEmptyZip() {
    ZipOutputStream(Files.newOutputStream(this)).use {}
  }

}