/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

/**
 * Flat, parser-agnostic view of the plugin descriptor values that [PluginDescriptorValidator] and the
 * descriptor verifiers in [com.jetbrains.plugin.structure.intellij.verifiers] check.
 *
 * Both descriptor pipelines are validated through this single view, so a check cannot silently apply to
 * one of them only: [PluginBeanView] maps the JAXB [com.jetbrains.plugin.structure.intellij.beans.PluginBean],
 * [PlatformDescriptorView] the platform parser's `RawPluginDescriptor` plus the lexical facts it drops.
 *
 * Some values are deliberately *lexical*, i.e. kept as written in XML, because the checks are about the
 * written form: see [ProductDescriptorView] and [DependencyView.optional].
 */
interface ValidatableDescriptor {
  val id: String?
  val name: String?
  val version: String?
  val url: String?
  val description: String?
  val changeNotes: String?

  /** `null` when the descriptor has no `<vendor>` element at all. */
  val vendor: VendorView?

  /** `null` when the descriptor has no `<idea-version>` element at all. */
  val ideaVersion: IdeaVersionView?

  /** `null` when the descriptor has no `<product-descriptor>` element at all. */
  val productDescriptor: ProductDescriptorView?

  /** `<depends>` declarations. */
  val dependencies: List<DependencyView>

  /** `<module value="...">` plugin aliases. */
  val pluginAliases: List<String>

  data class VendorView(val name: String?, val url: String?, val email: String?)

  data class IdeaVersionView(val sinceBuild: String?, val untilBuild: String?)

  /**
   * `<product-descriptor>` attributes as written: `release-date` must be validated against its written
   * `yyyyMMdd` form, `release-version` against its leading-zero/single-digit shape, and `eap`/`optional`
   * against their exact `true`/`false` spelling.
   */
  data class ProductDescriptorView(
    val code: String?,
    val releaseDate: String?,
    val releaseVersion: String?,
    val eap: String?,
    val optional: String?
  )

  /**
   * @param optional `null` when the `optional` attribute is absent, `false` only when it is explicitly
   *   written as false - which is what `SuperfluousNonOptionalDependencyDeclaration` is about.
   */
  data class DependencyView(val pluginId: String?, val optional: Boolean?, val configFile: String?)
}
