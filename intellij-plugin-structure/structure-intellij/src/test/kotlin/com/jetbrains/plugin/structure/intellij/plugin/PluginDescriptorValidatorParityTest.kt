/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.extractor.PluginBeanExtractor
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Builds [PluginBeanView] and [PlatformDescriptorView] from the same descriptor and asserts that
 * [PluginDescriptorValidator] reports the same problems for both.
 */
class PluginDescriptorValidatorParityTest {
  @Test
  fun `well-formed descriptor`() {
    assertParity(
      descriptor(
        """
          <product-descriptor code="PEXAMPLE" release-date="20260101" release-version="10"/>
          <depends>com.intellij.modules.platform</depends>
          <depends optional="true" config-file="optional.xml">optional.plugin</depends>
        """.trimIndent()
      ),
      expectNoProblems = true
    )
  }

  @Test
  fun `explicit optional false, including a duplicate id`() {
    assertParity(
      descriptor(
        """
          <depends optional="false">explicit.plugin</depends>
          <depends>implicit.plugin</depends>
          <depends optional="false">duplicate.plugin</depends>
          <depends>duplicate.plugin</depends>
          <depends optional="0">zero.plugin</depends>
        """.trimIndent()
      )
    )
  }

  @Test
  fun `optional dependency config-file problems`() {
    assertParity(
      descriptor(
        """
          <depends optional="true">no.config.plugin</depends>
          <depends optional="true" config-file=" ">blank.config.plugin</depends>
          <depends optional="true" config-file="shared.xml">first.plugin</depends>
          <depends optional="true" config-file="shared.xml">second.plugin</depends>
        """.trimIndent()
      )
    )
  }

  @Test
  fun `blank dependency id and plugin alias`() {
    assertParity(
      descriptor(
        """
          <depends/>
          <module value=""/>
        """.trimIndent()
      )
    )
  }

  @Test
  fun `eap spellings`() {
    for (eap in listOf("true", "false", "yes", "TRUE", "1", "")) {
      assertParity(
        descriptor("""<product-descriptor code="PEXAMPLE" release-date="20260101" release-version="10" eap="$eap"/>"""),
        expectNoProblems = eap == "true" || eap == "false"
      )
    }
  }

  @Test
  fun `product descriptor problems`() {
    assertParity(
      descriptor(
        """<product-descriptor code="PEXAMPLE_WAY_TOO_LONG" release-date="29990101" release-version="1" optional="false"/>"""
      )
    )
  }

  @Test
  fun `missing product descriptor attributes`() {
    assertParity(descriptor("""<product-descriptor/>"""))
  }

  @Test
  fun `missing vendor and idea-version`() {
    assertParity(
      """
        <idea-plugin>
          <id>example.plugin</id>
          <name>Example</name>
          <version>1.0</version>
        </idea-plugin>
      """.trimIndent()
    )
  }

  @Test
  fun `template values and bad build range`() {
    assertParity(
      """
        <idea-plugin>
          <id>com.your.company.unique.plugin.id</id>
          <name>Plugin display name here</name>
          <version>1.0</version>
          <vendor url="https://www.yourcompany.com" email="support@yourcompany.com">YourCompany</vendor>
          <idea-version since-build="IC-252.*" until-build="999"/>
          <change-notes>Add change notes here</change-notes>
        </idea-plugin>
      """.trimIndent()
    )
  }

  @Test
  fun `lexical facts contributed by an xinclude`() {
    val included = """
      <idea-plugin>
        <product-descriptor code="PEXAMPLE" release-date="20260101" release-version="1" eap="yes"/>
        <depends optional="false">included.plugin</depends>
        <depends optional="true" config-file="shared.xml">first.plugin</depends>
      </idea-plugin>
    """.trimIndent()
    val root = descriptor(
      """
        <depends optional="true" config-file="shared.xml">second.plugin</depends>
        <xi:include xmlns:xi="http://www.w3.org/2001/XInclude" href="included.xml"/>
      """.trimIndent()
    )
    // What XIncluder would have spliced for the JAXB path.
    val inlined = descriptor(
      """
        <depends optional="true" config-file="shared.xml">second.plugin</depends>
        <product-descriptor code="PEXAMPLE" release-date="20260101" release-version="1" eap="yes"/>
        <depends optional="false">included.plugin</depends>
        <depends optional="true" config-file="shared.xml">first.plugin</depends>
      """.trimIndent()
    )
    val platformProblems = assertParity(
      beanXml = inlined,
      platformXml = root,
      includes = mapOf("META-INF/included.xml" to included)
    )
    assertEquals(4, platformProblems.size)
  }

  /**
   * @return the (shared) problem messages.
   */
  private fun assertParity(
    beanXml: String,
    platformXml: String = beanXml,
    includes: Map<String, String> = emptyMap(),
    expectNoProblems: Boolean = false
  ): List<String> {
    val beanProblems = validate(PluginBeanView(PluginBeanExtractor.extractPluginBean(JDOMUtil.loadDocument(beanXml.byteInputStream()))))
    val platformProblems = validate(parsePlatform(platformXml, includes))
    assertEquals(beanProblems, platformProblems)
    if (expectNoProblems) {
      assertEquals(emptyList<String>(), beanProblems)
    } else {
      assertTrue("Fixture is expected to report problems", beanProblems.isNotEmpty())
    }
    return platformProblems
  }

  private fun parsePlatform(xml: String, includes: Map<String, String>): ValidatableDescriptor {
    val resolver = object : ResourceResolver {
      override fun resolveResource(relativePath: String, basePath: Path): ResourceResolver.Result {
        val content = includes[relativePath] ?: return ResourceResolver.Result.NotFound
        return ResourceResolver.Result.Found(
          Paths.get("/plugin").resolve(relativePath),
          content.byteInputStream(),
          description = "plugin.jar!/$relativePath"
        )
      }
    }
    val context = ValidationContext(DESCRIPTOR_PATH, AnyProblemToWarningPluginCreationResultResolver)
    val result = PlatformPluginDescriptorParser().parse(
      JDOMUtil.loadDocument(xml.byteInputStream()),
      Paths.get("/plugin"),
      resolver,
      DESCRIPTOR_PATH,
      "plugin.jar",
      context
    ) ?: throw AssertionError("Expected the platform descriptor to parse: ${context.problems}")
    return result.view
  }

  /** Sorted messages: the documents an include contributes are not in splice order. */
  private fun validate(descriptor: ValidatableDescriptor): List<String> {
    val context = ValidationContext(DESCRIPTOR_PATH, AnyProblemToWarningPluginCreationResultResolver)
    PluginDescriptorValidator().validate(descriptor, context, validateDescriptor = true)
    return context.problems.map { "${it.level}: ${it.message}" }.sorted()
  }

  private fun descriptor(extra: String) = """
    <idea-plugin>
      <id>example.plugin</id>
      <name>Example</name>
      <version>1.0</version>
      <vendor>Example Vendor</vendor>
      <description>A long enough description of this example plugin, written in English for the validator.</description>
      <idea-version since-build="252.0"/>
      $extra
    </idea-plugin>
  """.trimIndent()

  private companion object {
    const val DESCRIPTOR_PATH = "META-INF/plugin.xml"
  }
}
