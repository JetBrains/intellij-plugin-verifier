/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.intellij.platform.pluginSystem.parser.impl.RawPluginDescriptor
import com.jetbrains.plugin.structure.intellij.plugin.ValidatableDescriptor.*
import org.jdom2.Document
import org.jdom2.Element

/**
 * [ValidatableDescriptor] over the platform parser's [RawPluginDescriptor].
 *
 * [RawPluginDescriptor] is the semantic authority: values, and the order of repeated elements, come from
 * it. It does however drop lexical information some checks are about - `product-descriptor@eap` has no
 * field at all, `release-date`/`release-version` are parsed into a `LocalDate`/`Int`, whether
 * `depends@optional="false"` was explicit is gone, and `<depends>`/`<module>` declarations with a blank
 * value are skipped. That is recovered from [sourceDocuments]: the root document and the XInclude
 * documents the platform parser actually loaded (see
 * [RecordingXIncludeLoader][com.jetbrains.plugin.structure.intellij.xinclude.RecordingXIncludeLoader]).
 *
 * The documents are a set, not the spliced descriptor - their order does not reflect where each include
 * was spliced in - so they are only consulted for the presence and spelling of elements and attributes,
 * and wherever a value survives in [RawPluginDescriptor], that value wins.
 */
internal class PlatformDescriptorView(
  private val raw: RawPluginDescriptor,
  sourceDocuments: List<Document>
) : ValidatableDescriptor {

  /** Direct children of `<idea-plugin>` across all source documents. */
  private val descriptorChildren: List<Element> = sourceDocuments.flatMap { document ->
    document.rootElement.takeIf { it.name == IDEA_PLUGIN_ELEMENT }?.children.orEmpty()
  }

  override val id: String?
    get() = raw.id
  override val name: String?
    get() = raw.name
  override val version: String?
    get() = raw.version
  override val url: String?
    get() = raw.url
  override val description: String?
    get() = raw.description
  override val changeNotes: String?
    get() = raw.changeNotes

  override val vendor: VendorView? =
    if (raw.vendor != null || raw.vendorUrl != null || raw.vendorEmail != null || hasChild(VENDOR_ELEMENT)) {
      VendorView(raw.vendor, raw.vendorUrl, raw.vendorEmail)
    } else {
      null
    }

  override val ideaVersion: IdeaVersionView? =
    if (raw.sinceBuild != null || raw.untilBuild != null || hasChild(IDEA_VERSION_ELEMENT)) {
      IdeaVersionView(raw.sinceBuild, raw.untilBuild)
    } else {
      null
    }

  /**
   * The code comes from [raw]; the rest is lexical. Should several documents declare a
   * `<product-descriptor>`, the one whose `code` matches [RawPluginDescriptor.productCode] is preferred.
   */
  override val productDescriptor: ProductDescriptorView? = run {
    val candidates = children(PRODUCT_DESCRIPTOR_ELEMENT)
    val element = candidates.firstOrNull { it.getAttributeValue(CODE_ATTRIBUTE)?.trim() == raw.productCode }
      ?: candidates.firstOrNull()
      ?: return@run null
    ProductDescriptorView(
      code = raw.productCode,
      releaseDate = element.getAttributeValue(RELEASE_DATE_ATTRIBUTE),
      releaseVersion = element.getAttributeValue(RELEASE_VERSION_ATTRIBUTE),
      eap = element.getAttributeValue(EAP_ATTRIBUTE),
      optional = element.getAttributeValue(OPTIONAL_ATTRIBUTE)
    )
  }

  /**
   * [RawPluginDescriptor.depends] in the parser's order. `optional = false` is restored for as many
   * non-optional declarations of an ID as the documents hold explicitly non-optional ones, so that any
   * occurrence written with `optional="false"` is reported, without over-reporting duplicates.
   *
   * `<depends>` with a blank ID, which the library skips, are appended as written; only their ID and
   * `config-file` are ever checked.
   */
  override val dependencies: List<DependencyView> = run {
    val dependsElements = children(DEPENDS_ELEMENT)
    val explicitlyNonOptional = dependsElements
      .filter { it.getAttributeValue(OPTIONAL_ATTRIBUTE)?.trim() in FALSE_SPELLINGS }
      .groupingBy { it.text.trim() }
      .eachCount()
      .toMutableMap()
    val declared = raw.depends.map { depends ->
      val optional = when {
        depends.isOptional -> true
        explicitlyNonOptional.consume(depends.pluginId.trim()) -> false
        else -> null
      }
      DependencyView(depends.pluginId, optional, depends.configFile)
    }
    val blank = dependsElements
      .filter { it.text.isBlank() }
      .map { DependencyView(it.text, optional = null, configFile = it.getAttributeValue(CONFIG_FILE_ATTRIBUTE)) }
    declared + blank
  }

  /** [RawPluginDescriptor.pluginAliases], plus the blank `<module value="">` the library skips. */
  override val pluginAliases: List<String> = raw.pluginAliases + children(MODULE_ELEMENT)
    .mapNotNull { it.getAttributeValue(MODULE_VALUE_ATTRIBUTE) }
    .filter { it.isBlank() }

  private fun hasChild(name: String) = descriptorChildren.any { it.name == name }

  private fun children(name: String) = descriptorChildren.filter { it.name == name }

  private fun MutableMap<String, Int>.consume(key: String): Boolean {
    val count = this[key] ?: return false
    if (count <= 1) remove(key) else this[key] = count - 1
    return true
  }

  private companion object {
    const val IDEA_PLUGIN_ELEMENT = "idea-plugin"
    const val VENDOR_ELEMENT = "vendor"
    const val IDEA_VERSION_ELEMENT = "idea-version"
    const val PRODUCT_DESCRIPTOR_ELEMENT = "product-descriptor"
    const val DEPENDS_ELEMENT = "depends"
    const val MODULE_ELEMENT = "module"
    const val CODE_ATTRIBUTE = "code"
    const val RELEASE_DATE_ATTRIBUTE = "release-date"
    const val RELEASE_VERSION_ATTRIBUTE = "release-version"
    const val EAP_ATTRIBUTE = "eap"
    const val OPTIONAL_ATTRIBUTE = "optional"
    const val CONFIG_FILE_ATTRIBUTE = "config-file"
    const val MODULE_VALUE_ATTRIBUTE = "value"

    /**
     * Lexical forms of a false `depends@optional`: the library reads it as a typed XML boolean, and JAXB
     * reads `0` as false too.
     */
    val FALSE_SPELLINGS = setOf("false", "0")
  }
}
