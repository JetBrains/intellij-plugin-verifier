package com.jetbrains.plugin.structure.intellij.platform

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import junit.framework.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProductInfoParserTest {
  private lateinit var jackson: ObjectMapper

  @Before
  fun setUp() {
    jackson = ObjectMapper()
  }

  @Test
  fun `product-info JSON is loaded`() {
    val productInfoUrl = ProductInfoParserTest::class.java.getResource("product-info.json")
      ?: throw AssertionError("product-info.json not found in classpath")

    val productInfo = jackson.readValue<ProductInfo>(productInfoUrl)

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
      .filterIsInstance<Layout.ModuleV2>()
    assertEquals(379, v2Modules.size)
    assertEquals(4, v2Modules.filter { it.classPaths.isNotEmpty() }.size)

    val v2ProductModules = productInfo.layout
      .filterIsInstance<Layout.ProductModuleV2>()
    assertEquals(33, v2ProductModules.size)
    assertEquals(0, v2ProductModules.filter {
      @Suppress("SENSELESS_COMPARISON")
      it.classPaths == null
    }.size)
  }

  @Test
  fun `product-info JSON is serialized`() {
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
        Layout.Plugin("Coverage", listOf(
          "plugins/java-coverage/lib/java-coverage.jar",
          "plugins/java-coverage/lib/java-coverage-rt.jar",
        )),
        Layout.PluginAlias("com.intellij.modules.json")
      )
    )
    assertEquals(expectedJson, jackson.writeValueAsString(productInfo))
  }
}