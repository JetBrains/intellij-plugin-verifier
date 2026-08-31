/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.plugin.descriptors.DescriptorResource
import com.jetbrains.plugin.structure.intellij.problems.AnyProblemToWarningPluginCreationResultResolver
import com.jetbrains.plugin.structure.intellij.resources.ResourceResolver
import com.jetbrains.plugin.structure.intellij.utils.JDOMUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI
import java.nio.file.Path

/**
 * Covers [PluginCreator.shouldUsePlatformParser] - which of the two descriptor parsers a plugin gets.
 *
 * The rule is exercised directly rather than through a parsed plugin on purpose: the platform parser is
 * currently forced on for every descriptor for corpus evaluation
 * (`FORCE_PLATFORM_PARSER_FOR_EVERY_DESCRIPTOR`, see PARSER_PLAN.md Step 6), so
 * [PluginCreator.usedPlatformParser] cannot observe the rule's verdict while that is in place. These
 * tests keep the rule itself honest in the meantime.
 */
class PlatformParserSelectionRuleTest {

  // --- until-build drives the decision -------------------------------------------------------

  @Test
  fun `until build below the conditional-include removal baseline stays on the JAXB path`() {
    assertRejected(sinceBuild = "241.0", untilBuild = "261.*")
    assertRejected(sinceBuild = "262.2500", untilBuild = "262.2500")
    assertRejected(sinceBuild = "262.0", untilBuild = "262.*")
  }

  @Test
  fun `until build at or past the conditional-include removal baseline selects the platform parser`() {
    assertSelected(sinceBuild = "241.0", untilBuild = "263.*")
    assertSelected(sinceBuild = "241.0", untilBuild = "300.*")
    assertSelected(sinceBuild = "241.0", untilBuild = "999.*")
  }

  @Test
  fun `a bare until build baseline is compared as a baseline, not as a build number`() {
    // IdeVersionImpl treats a single component <= 2000 as a baseline, giving components [262, 0].
    assertRejected(sinceBuild = "241.0", untilBuild = "262")
    assertSelected(sinceBuild = "241.0", untilBuild = "263")
  }

  @Test
  fun `a product code on until build is stripped before comparing`() {
    assertRejected(sinceBuild = "241.0", untilBuild = "IU-262.*")
    assertSelected(sinceBuild = "241.0", untilBuild = "IU-263.*")
  }

  @Test
  fun `since build is irrelevant once until build is present`() {
    // A plugin pinned to 262 is by any normal reading NEWER than one declaring since=260 with no
    // upper bound, yet it is the pinned one that keeps the old parser: only the upper bound can
    // overlap [263, infinity).
    assertRejected(sinceBuild = "262.2500", untilBuild = "262.2500")
    assertSelected(sinceBuild = "203.0", untilBuild = "263.0")
    // Even a nonsensical since > until does not change the answer.
    assertSelected(sinceBuild = "299.0", untilBuild = "263.0")
    assertRejected(sinceBuild = "299.0", untilBuild = "241.0")
  }

  // --- no until-build: the since-build trust floor --------------------------------------------

  @Test
  fun `an unbounded descriptor is selected only from the trust floor upwards`() {
    assertSelected(sinceBuild = "252.0", untilBuild = null)
    assertSelected(sinceBuild = "260.0", untilBuild = null)
    assertRejected(sinceBuild = "251.0", untilBuild = null)
    assertRejected(sinceBuild = "241.0", untilBuild = null)
  }

  @Test
  fun `a descriptor with no idea-version at all stays on the JAXB path`() {
    assertFalse(selectsPlatformParser(ideaVersion = null))
  }

  @Test
  fun `a descriptor with an empty idea-version element stays on the JAXB path`() {
    assertFalse(selectsPlatformParser(ideaVersion = "<idea-version/>"))
  }

  // --- malformed bounds ------------------------------------------------------------------------

  @Test
  fun `a malformed until build falls through to the since build clause`() {
    // A malformed upper bound is not a declaration of compatibility, so it must not be read as one.
    // Selection runs before validation and cannot depend on it.
    assertRejected(sinceBuild = "241.0", untilBuild = "not-a-version")
    assertSelected(sinceBuild = "252.0", untilBuild = "not-a-version")
  }

  @Test
  fun `a malformed since build on an unbounded descriptor stays on the JAXB path`() {
    assertRejected(sinceBuild = "not-a-version", untilBuild = null)
  }

  // --- inheritance ------------------------------------------------------------------------------

  @Test
  fun `a content module inherits its parent's choice rather than being judged on its own`() {
    // The module descriptor declares no <idea-version> at all, so judged on its own it would always
    // be rejected. Anything else can only have been inherited.
    val standalone = createPlugin("<idea-plugin/>")
    assertFalse(
      "a descriptor with no <idea-version> must be rejected when judged on its own",
      standalone.shouldUsePlatformParser(document("<idea-plugin/>"))
    )

    val parent = createPlugin(pluginXml(sinceBuild = "241.0", untilBuild = "263.*"))
    val child = PluginCreator.createPlugin(
      "module.jar", "some.module.xml", parent, false,
      document("<idea-plugin/>"), Path.of("some.module.xml"), NOT_FOUND_RESOLVER
    )
    assertEquals(
      "the content module must take the parent's choice, not its own descriptor's",
      parent.usedPlatformParser,
      child.shouldUsePlatformParser(document("<idea-plugin/>"))
    )
    assertEquals(parent.usedPlatformParser, child.usedPlatformParser)
  }

  @Test
  fun `an inline module inherits the containing descriptor's choice`() {
    val parent = createPlugin(pluginXml(sinceBuild = "241.0", untilBuild = "263.*"))
    assertEquals(parent.usedPlatformParser, inlineModuleOf(parent).usedPlatformParser)
  }

  @Test
  fun `an inline module inherits the containing descriptor's resource root`() {
    // It has no filesystem path of its own to derive one from, so without inheritance it would have no
    // resource root at all and could not resolve a single <xi:include>.
    val parent = createPlugin(pluginXml(sinceBuild = "241.0", untilBuild = "263.*"))
    assertEquals(parent.resourceRoot, inlineModuleOf(parent).resourceRoot)
  }

  // --- helpers ----------------------------------------------------------------------------------

  private fun inlineModuleOf(parent: PluginCreator): PluginCreator = PluginCreator.createPlugin(
    DescriptorResource("<idea-plugin/>".byteInputStream(), URI("$PLUGIN_URI#modules/some.module"), URI(PLUGIN_URI)),
    parent,
    document("<idea-plugin/>"),
    NOT_FOUND_RESOLVER,
    AnyProblemToWarningPluginCreationResultResolver
  )

  private fun assertSelected(sinceBuild: String?, untilBuild: String?) {
    assertTrue(
      "since-build=$sinceBuild until-build=$untilBuild must select the platform parser",
      selectsPlatformParser(ideaVersion(sinceBuild, untilBuild))
    )
  }

  private fun assertRejected(sinceBuild: String?, untilBuild: String?) {
    assertFalse(
      "since-build=$sinceBuild until-build=$untilBuild must stay on the JAXB path",
      selectsPlatformParser(ideaVersion(sinceBuild, untilBuild))
    )
  }

  private fun selectsPlatformParser(ideaVersion: String?): Boolean {
    val creator = createPlugin("<idea-plugin/>")
    return creator.shouldUsePlatformParser(document(pluginXml(ideaVersion)))
  }

  private fun ideaVersion(sinceBuild: String?, untilBuild: String?): String {
    val since = sinceBuild?.let { """ since-build="$it"""" } ?: ""
    val until = untilBuild?.let { """ until-build="$it"""" } ?: ""
    return "<idea-version$since$until/>"
  }

  private fun pluginXml(sinceBuild: String?, untilBuild: String?) = pluginXml(ideaVersion(sinceBuild, untilBuild))

  private fun pluginXml(ideaVersion: String?) = """
    <idea-plugin>
      <id>com.example.plugin</id>
      <name>Example</name>
      <version>1.0</version>
      <vendor>JetBrains</vendor>
      ${ideaVersion ?: ""}
    </idea-plugin>
  """.trimIndent()

  private fun document(xml: String) = xml.byteInputStream().use { JDOMUtil.loadDocument(it) }

  private fun createPlugin(pluginXml: String): PluginCreator = PluginCreator.createPlugin(
    "plugin.jar", "plugin.xml", null, false,
    document(pluginXml), Path.of("META-INF", "plugin.xml"), NOT_FOUND_RESOLVER
  )

  private companion object {
    const val PLUGIN_URI = "file:///plugins/somePlugin/META-INF/plugin.xml"

    val NOT_FOUND_RESOLVER = object : ResourceResolver {
      override fun resolveResource(relativePath: String, basePath: Path) = ResourceResolver.Result.NotFound
    }
  }
}
