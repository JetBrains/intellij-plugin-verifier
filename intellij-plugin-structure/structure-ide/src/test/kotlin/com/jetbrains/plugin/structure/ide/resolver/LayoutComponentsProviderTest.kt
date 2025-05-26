package com.jetbrains.plugin.structure.ide.resolver

import com.jetbrains.plugin.structure.ide.layout.IdeRelativePath
import com.jetbrains.plugin.structure.ide.layout.MissingClasspathFileInLayoutComponentException
import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import com.jetbrains.plugin.structure.ide.problem.LayoutComponentHasNonExistentClasspath
import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class LayoutComponentsProviderTest {
  @Test
  fun `product info with a single layout component that is valid is correctly parsed and does not fail`() {
    val idePath: Path = Files.createTempDirectory(/* prefix = */ null)
    idePath.resolve("somePlugin.jar").also(Files::createFile)

    val productInfo = ProductInfoParser()
      .parse(productInfoJson.byteInputStream(), "Unit Test Constant String")

    val provider = ValidatingLayoutComponentsProvider(MissingLayoutFileMode.FAIL)
    val layoutComponents = provider.resolveLayoutComponents(productInfo, idePath)

    assertEquals(1, layoutComponents.toList().size)
    val somePlugin = layoutComponents.first()
    with(somePlugin.layoutComponent) {
      assertEquals("com.jetbrains.somePlugin", name)
      assertTrue(this is LayoutComponent.Plugin)
      this as LayoutComponent.Plugin
      assertEquals(1, classPaths.size)
      assertEquals("somePlugin.jar", classPaths[0])
    }
    assertEquals(0, layoutComponents.problems.size)
  }

  @Test
  fun `product info with a single layout component that has one valid and two missing classpath elements will fail`() {
    assertThrows(MissingClasspathFileInLayoutComponentException::class.java) {
      val (idePath, productInfo) = parseProductInfo(productInfoJsonWithMissingJarInClasspath)
      val provider = ValidatingLayoutComponentsProvider(MissingLayoutFileMode.FAIL)
      provider.resolveLayoutComponents(productInfo, idePath)
    }
  }

  @Test
  fun `product info with a single layout component that has one valid and two missing classpath elements will ignore errors`() {
    val (idePath, productInfo) = parseProductInfo(productInfoJsonWithMissingJarInClasspath)
    val provider = ValidatingLayoutComponentsProvider(MissingLayoutFileMode.IGNORE)
    val layoutComponents = provider.resolveLayoutComponents(productInfo, idePath)

    assertEquals(1, layoutComponents.toList().size)
    val somePlugin = layoutComponents.first()
    with(somePlugin.layoutComponent) {
      assertEquals("com.jetbrains.somePlugin", name)
      assertTrue(this is LayoutComponent.Plugin)
      this as LayoutComponent.Plugin
      assertEquals(3, classPaths.size)
      assertEquals(listOf("somePlugin.jar", "missingComponentOne.jar", "missingComponentTwo.jar"), classPaths)
    }
    //FIXME should not be ignored
    assertEquals(0, layoutComponents.problems.size)
  }

  @Test
  fun `product info with a single layout component that has one valid and two missing classpath elements will skip missing classpath elements`() {
    val (idePath, productInfo) = parseProductInfo(productInfoJsonWithMissingJarInClasspath)
    val provider = ValidatingLayoutComponentsProvider(MissingLayoutFileMode.SKIP_CLASSPATH)
    val layoutComponents = provider.resolveLayoutComponents(productInfo, idePath)

    assertEquals(1, layoutComponents.toList().size)
    val somePlugin = layoutComponents.first()
    with(somePlugin.layoutComponent) {
      assertEquals("com.jetbrains.somePlugin", name)
      assertTrue(this is LayoutComponent.Plugin)
      this as LayoutComponent.Plugin
      assertEquals(1, classPaths.size)
      assertEquals(listOf("somePlugin.jar"), classPaths)
    }
    with(layoutComponents.problems) {
      assertEquals(1, size)
      val ideProblem = first()
      assertTrue(ideProblem is LayoutComponentHasNonExistentClasspath)
      ideProblem as LayoutComponentHasNonExistentClasspath
      ideProblem.layoutComponentName == "com.jetbrains.somePlugin"
      val expectedMissingClasspathElements = listOf("missingComponentOne.jar", "missingComponentTwo.jar").map {
        IdeRelativePath(idePath, Path.of(it))
      }
      assertEquals(expectedMissingClasspathElements, ideProblem.offendingClasspathElements)
    }
  }
  private fun parseProductInfo(jsonSource: String): Pair<Path, ProductInfo> {
    val idePath: Path = Files.createTempDirectory(/* prefix = */ null)
    idePath.resolve("somePlugin.jar").also(Files::createFile)

    val productInfo = ProductInfoParser()
      .parse(jsonSource.byteInputStream(), "Unit Test Constant String")

    return idePath to productInfo
  }

  private val productInfoJson = """
    {
      "name": "JetBrains Rider",
      "version": "2024.2",
      "versionSuffix": "EAP 5",
      "buildNumber": "242.19533.58",
      "productCode": "RD",
      "dataDirectoryName": "Rider2024.2",
      "svgIconPath": "bin/rider.svg",
      "productVendor": "JetBrains",
      "launch": [],
      "customProperties": [],
      "bundledPlugins": [],
      "modules": [],
      "fileExtensions": [],
      "layout": [
        {
          "name": "com.jetbrains.somePlugin",
          "kind": "plugin",
          "classPath": [
            "somePlugin.jar"
          ]
        }     
      ]
    }
    """.trimIndent()

  private val productInfoJsonWithMissingJarInClasspath = """
    {
      "name": "JetBrains Rider",
      "version": "2024.2",
      "versionSuffix": "EAP 5",
      "buildNumber": "242.19533.58",
      "productCode": "RD",
      "dataDirectoryName": "Rider2024.2",
      "svgIconPath": "bin/rider.svg",
      "productVendor": "JetBrains",
      "launch": [],
      "customProperties": [],
      "bundledPlugins": [],
      "modules": [],
      "fileExtensions": [],
      "layout": [
        {
          "name": "com.jetbrains.somePlugin",
          "kind": "plugin",
          "classPath": [
            "somePlugin.jar",
            "missingComponentOne.jar",
            "missingComponentTwo.jar"
          ]
        }     
      ]
    }
    """.trimIndent()
}