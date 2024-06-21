package com.jetbrains.plugin.structure.intellij.platform

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductInfoParserTest {
  private val productInfoParser = ProductInfoParser()

  @Test
  fun `product-info JSON is loaded`() {
    val productInfoUrl = ProductInfoParserTest::class.java.getResource("product-info.json")
      ?: throw AssertionError("product-info.json not found in classpath")

    val productInfo = productInfoParser.parse(productInfoUrl)

    assertEquals("IntelliJ IDEA", productInfo.name)
    assertEquals("2024.2", productInfo.version)
    assertEquals("EAP", productInfo.versionSuffix)
    assertEquals("242.10180.25", productInfo.buildNumber)
    assertEquals("IU", productInfo.productCode)
    assertEquals("IntelliJIdea2024.2", productInfo.dataDirectoryName)
    assertEquals("bin/idea.svg", productInfo.svgIconPath)
    assertEquals("JetBrains", productInfo.productVendor)
    assertEquals(163, productInfo.bundledPlugins.size)
    assertEquals(48, productInfo.modules.size)

    assertEquals(623, productInfo.layout.size)

    val v2Modules = productInfo.layout
      .filterIsInstance<LayoutComponent.ModuleV2>()
    assertEquals(379, v2Modules.size)
    assertEquals(4, v2Modules.filter { it.classPaths.isNotEmpty() }.size)

    val v2ProductModules = productInfo.layout
      .filterIsInstance<LayoutComponent.ProductModuleV2>()
    assertEquals(33, v2ProductModules.size)
    assertEquals(0, v2ProductModules.filter {
      @Suppress("SENSELESS_COMPARISON")
      it.classPaths == null
    }.size)
  }

  @Test
  fun `product-info JSON is serialized`() {
    val jackson = ObjectMapper()

    val expectedJson = """
          {"name":"name","version":"version","versionSuffix":"versioNnuffix","buildNumber":"buildNumber","productCode":"productCode","dataDirectoryName":"dataDirectoryName","svgIconPath":"svgIconPath","productVendor":"productVendor","bundledPlugins":[],"modules":[],"layout":[{"name":"Coverage","kind":"plugin","classPaths":["plugins/java-coverage/lib/java-coverage.jar","plugins/java-coverage/lib/java-coverage-rt.jar"]},{"name":"com.intellij.modules.json","kind":"pluginAlias"}]}
    """.trimIndent()
    val productInfo = ProductInfo(
      "name",
      "version",
      "versioNnuffix",
      "buildNumber",
      "productCode",
      "dataDirectoryName",
      "svgIconPath",
      "productVendor",
      emptyList(),
      emptyList(),
      listOf(
        LayoutComponent.Plugin("Coverage", listOf(
          "plugins/java-coverage/lib/java-coverage.jar",
          "plugins/java-coverage/lib/java-coverage-rt.jar",
        )),
        LayoutComponent.PluginAlias("com.intellij.modules.json")
      )
    )
    assertEquals(expectedJson, jackson.writeValueAsString(productInfo))
  }
}