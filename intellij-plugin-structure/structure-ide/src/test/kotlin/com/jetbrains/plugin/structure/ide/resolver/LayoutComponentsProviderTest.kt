package com.jetbrains.plugin.structure.ide.resolver

import com.jetbrains.plugin.structure.ide.layout.MissingLayoutFileMode
import com.jetbrains.plugin.structure.intellij.platform.ProductInfoParser
import org.junit.Assert.assertEquals
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

    val provider = LayoutComponentsProvider(MissingLayoutFileMode.FAIL)
    val layoutComponents = provider.resolveLayoutComponents(productInfo, idePath)

    assertEquals(1, layoutComponents.toList().size)
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
}