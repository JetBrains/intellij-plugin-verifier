/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.intellij.platform.pluginSystem.parser.impl.PluginDescriptorReaderContext
import com.intellij.platform.pluginSystem.parser.impl.RawPluginDescriptor
import com.intellij.platform.pluginSystem.parser.impl.parsePluginXml
import com.intellij.util.xml.dom.NoOpXmlInterner
import com.jetbrains.plugin.structure.base.problems.NotBoolean
import com.jetbrains.plugin.structure.base.problems.ReusedDescriptorInMultipleDependencies
import com.jetbrains.plugin.structure.intellij.plugin.ValidatableDescriptor.DependencyView
import com.jetbrains.plugin.structure.intellij.plugin.ValidatableDescriptor.ProductDescriptorView
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.problems.SuperfluousNonOptionalDependencyDeclaration
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import org.jdom2.Document
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Paths

class PlatformDescriptorViewTest {
  @Test
  fun `restores product eap validation and conversion`() {
    val xml = descriptor("""<product-descriptor code="PC" release-date="20260101" release-version="1" eap="yes"/>""")
    val view = PlatformDescriptorView(parse(xml), listOf(load(xml)))

    assertEquals(ProductDescriptorView("PC", "20260101", "1", "yes", null), view.productDescriptor)
    assertTrue(validate(view).problems.any { it is NotBoolean })

    val trueXml = descriptor("""<product-descriptor code="PC" release-date="20260101" release-version="1" eap="true"/>""")
    val trueDocument = load(trueXml)
    val trueRaw = parse(trueXml)
    val target = IdePluginImpl()
    RawPluginDescriptorToIdePluginConverter().convert(
      trueRaw,
      PlatformDescriptorView(trueRaw, listOf(trueDocument)),
      trueDocument,
      null,
      {},
      target
    )
    assertTrue(target.productDescriptor?.eap == true)
  }

  @Test
  fun `restores explicit false and reused config file dependency checks across sources`() {
    val rootXml = descriptor("""<depends optional="false" config-file="shared.xml">first.plugin</depends>""")
    val includeXml = """
      <idea-plugin>
        <depends optional="true" config-file="shared.xml">second.plugin</depends>
      </idea-plugin>
    """.trimIndent()
    val raw = parse(
      descriptor(
        """
          <depends optional="false" config-file="shared.xml">first.plugin</depends>
          <depends optional="true" config-file="shared.xml">second.plugin</depends>
        """.trimIndent()
      )
    )
    val view = PlatformDescriptorView(raw, listOf(load(rootXml), load(includeXml)))

    assertEquals(
      listOf(
        DependencyView("first.plugin", false, "shared.xml"),
        DependencyView("second.plugin", true, "shared.xml")
      ),
      view.dependencies
    )
    val context = validate(view)
    assertTrue(context.problems.any { it is SuperfluousNonOptionalDependencyDeclaration })
    assertTrue(context.problems.any { it is ReusedDescriptorInMultipleDependencies })
  }

  @Test
  fun `absent optional attribute stays distinguishable from explicit false`() {
    val xml = descriptor(
      """
        <depends>implicit.plugin</depends>
        <depends optional="false">explicit.plugin</depends>
        <depends optional="false">duplicate.plugin</depends>
        <depends>duplicate.plugin</depends>
      """.trimIndent()
    )
    val view = PlatformDescriptorView(parse(xml), listOf(load(xml)))

    assertEquals(
      listOf(
        DependencyView("implicit.plugin", null, null),
        DependencyView("explicit.plugin", false, null),
        DependencyView("duplicate.plugin", false, null),
        DependencyView("duplicate.plugin", null, null)
      ),
      view.dependencies
    )
  }

  @Test
  fun `recovers blank declarations the library skips`() {
    val xml = descriptor(
      """
        <depends config-file="blank.xml"/>
        <module value=""/>
      """.trimIndent()
    )
    val view = PlatformDescriptorView(parse(xml), listOf(load(xml)))

    assertEquals(listOf(DependencyView("", null, "blank.xml")), view.dependencies)
    assertEquals(listOf(""), view.pluginAliases)
  }

  @Test
  fun `absent vendor, idea-version and product-descriptor are absent from the view`() {
    val xml = """
      <idea-plugin>
        <id>example.plugin</id>
      </idea-plugin>
    """.trimIndent()
    val view = PlatformDescriptorView(parse(xml), listOf(load(xml)))

    assertNull(view.vendor)
    assertNull(view.ideaVersion)
    assertNull(view.productDescriptor)
  }

  @Test
  fun `platform parser exposes a view over the includes it actually loads`() {
    val rootXml = descriptor(
      """<xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="dependencies.xml"/>"""
    )
    val includedXml = """
      <idea-plugin>
        <depends optional="false">included.plugin</depends>
      </idea-plugin>
    """.trimIndent()
    val resolver = object : ResourceResolver {
      override fun resolveResource(relativePath: String, basePath: java.nio.file.Path): ResourceResolver.Result {
        return if (relativePath == "META-INF/dependencies.xml") {
          ResourceResolver.Result.Found(
            Paths.get("/plugin/META-INF/dependencies.xml"),
            includedXml.byteInputStream(),
            description = "plugin.jar!/META-INF/dependencies.xml"
          )
        } else {
          ResourceResolver.Result.NotFound
        }
      }
    }
    val context = ValidationContext("META-INF/plugin.xml", AnyProblemToWarningPluginCreationResultResolver)

    val result = PlatformPluginDescriptorParser().parse(
      load(rootXml),
      Paths.get("/plugin"),
      resolver,
      "META-INF/plugin.xml",
      "plugin.jar",
      context
    ) ?: throw AssertionError("Expected the platform descriptor to parse")
    PluginDescriptorValidator().validate(result.view, context, validateDescriptor = true)

    assertEquals(listOf(DependencyView("included.plugin", false, null)), result.view.dependencies)
    assertTrue(context.problems.any { it is SuperfluousNonOptionalDependencyDeclaration })
  }

  private fun validate(view: ValidatableDescriptor): ValidationContext {
    val context = ValidationContext("META-INF/plugin.xml", AnyProblemToWarningPluginCreationResultResolver)
    PluginDescriptorValidator().validate(view, context, validateDescriptor = true)
    return context
  }

  private fun load(xml: String): Document = JDOMUtil.loadDocument(xml.byteInputStream())

  private fun parse(xml: String): RawPluginDescriptor = parsePluginXml(
    xml.toByteArray(),
    "plugin.xml",
    object : PluginDescriptorReaderContext {
      override val interner = NoOpXmlInterner
      override val isMissingIncludeIgnored = false
    },
    null
  ).build()

  private fun descriptor(extra: String) = """
    <idea-plugin>
      <id>example.plugin</id>
      <name>Example</name>
      <version>1.0</version>
      <vendor>Example Vendor</vendor>
      <idea-version since-build="252.0"/>
      $extra
    </idea-plugin>
  """.trimIndent()
}
