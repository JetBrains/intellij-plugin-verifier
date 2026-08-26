/*
 * Copyright 2000-2026 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.intellij.platform.pluginSystem.parser.impl.RawPluginDescriptor
import com.intellij.platform.pluginSystem.parser.impl.elements.DependsElement
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.intellij.beans.IdeaVersionBean
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.verifiers.MAX_PROPERTY_LENGTH
import com.jetbrains.plugin.structure.intellij.verifiers.SINCE_BASELINE_LOWER_BOUND
import com.jetbrains.plugin.structure.intellij.verifiers.verifyIdeBuildComponentsRanges
import com.jetbrains.plugin.structure.intellij.version.IdeVersion

private val DEFAULT_TEMPLATE_NAMES = setOf("Plugin display name here", "My Framework Support", "Template", "Demo")

private val PLUGIN_NAME_RESTRICTED_WORDS = setOf(
  "plugin", "JetBrains", "IDEA", "PyCharm", "CLion", "AppCode", "DataGrip", "Fleet", "GoLand", "PhpStorm",
  "WebStorm", "Rider", "ReSharper", "TeamCity", "YouTrack", "RubyMine", "IntelliJ"
)

private const val MAX_VERSION_LENGTH = 64

/**
 * POC counterpart to [PluginBeanValidator], operating on [RawPluginDescriptor] (produced by
 * [PlatformPluginDescriptorParser]) instead of [com.jetbrains.plugin.structure.intellij.beans.PluginBean].
 *
 * NOT at full parity with [PluginBeanValidator] by design - this POC's goal is to test-run the new
 * parsing path against a large plugin corpus and see what breaks structurally (crashes, grossly wrong
 * field values), not to reproduce every last validation message byte-for-byte on day one. Checks that
 * are intentionally NOT ported yet (left as a follow-up once the parser/converter side has proven out):
 *  - HTML description validation ([validateDescriptionIsCorrect])
 *  - default change-notes / template text detection
 *  - product-descriptor sub-validations (product code length, release date range, release-version format)
 *    - note [RawPluginDescriptor] has no `eap` field at all, see PlatformPluginDescriptorParser's class doc
 *  - `pluginUntilBuildVerifier` / `ProductReleaseVersionVerifier` (both PluginBean-typed today)
 *  - full [PluginIdVerifier] parity (illegal-prefix / restricted-word-in-id checks) - only the
 *    blank/default-value/length checks are ported here
 */
internal class PlatformDescriptorValidator {

  fun validate(raw: RawPluginDescriptor, validationContext: ValidationContext, validateDescriptor: Boolean) {
    validationContext.validate(raw, validateDescriptor)
  }

  private fun ValidationContext.validate(raw: RawPluginDescriptor, validateDescriptor: Boolean) {
    if (validateDescriptor) {
      validateBeanUrl(raw.url)
      validateId(raw.id)
      validateName(raw.name)
      validateVersion(raw.version)
      validateVendor(raw)
      validateSinceBuild(raw.sinceBuild)
    }
    validateDependencies(raw.depends)
    validateModules(raw)
  }

  private fun ValidationContext.validatePropertyLength(propertyName: String, propertyValue: String, maxLength: Int) {
    if (propertyValue.length > maxLength) {
      registerProblem(TooLongPropertyValue(descriptorPath, propertyName, propertyValue.length, maxLength))
    }
  }

  private fun ValidationContext.validateBeanUrl(url: String?) {
    if (url != null) {
      validatePropertyLength("plugin url", url, MAX_PROPERTY_LENGTH)
    }
  }

  private fun ValidationContext.validateId(id: String?) {
    // Simplified vs. PluginIdVerifier: illegal-prefix / restricted-word checks are skipped for this POC.
    if (id == null) return
    when {
      id.isBlank() -> registerProblem(PropertyNotSpecified("id"))
      id == "com.your.company.unique.plugin.id" ->
        registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.ID, id))
      else -> validatePropertyLength("id", id, MAX_PROPERTY_LENGTH)
    }
  }

  private fun ValidationContext.validateName(name: String?) {
    when {
      name.isNullOrBlank() -> registerProblem(PropertyNotSpecified("name", descriptorPath))
      DEFAULT_TEMPLATE_NAMES.any { it.equals(name, true) } -> {
        registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.NAME, name))
      }
      else -> {
        val templateWord = PLUGIN_NAME_RESTRICTED_WORDS.find { name.contains(it, true) }
        if (templateWord != null) {
          registerProblem(TemplateWordInPluginName(descriptorPath, name, templateWord))
        }
        validatePropertyLength("name", name, MAX_NAME_LENGTH)
      }
    }
  }

  private fun ValidationContext.validateVersion(version: String?) {
    if (version.isNullOrEmpty()) {
      registerProblem(PropertyNotSpecified("version", descriptorPath))
    } else {
      validatePropertyLength("version", version, MAX_VERSION_LENGTH)
    }
  }

  // RawPluginDescriptor flattens vendor name/url/email onto the descriptor itself - there's no nested
  // vendor bean to null-check as a whole, so absence is "name is null" (mirrors old validateVendor's
  // "vendorBean == null" branch, which only ever fired when <vendor> was entirely missing).
  private fun ValidationContext.validateVendor(raw: RawPluginDescriptor) {
    val name = raw.vendor
    if (name.isNullOrBlank()) {
      registerProblem(VendorCannotBeEmpty(descriptorPath))
      return
    }

    if ("YourCompany" == name) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR, name))
    }
    validatePropertyLength("vendor", name, MAX_PROPERTY_LENGTH)

    // Bound to locals before comparing/using: RawPluginDescriptor is compiled in a different module,
    // so Kotlin won't smart-cast `raw.vendorUrl` etc. from a null-check straight to non-null String.
    val vendorUrl = raw.vendorUrl
    if ("https://www.yourcompany.com" == vendorUrl) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR_URL, vendorUrl))
    }
    vendorUrl?.let { validatePropertyLength("vendor url", it, MAX_PROPERTY_LENGTH) }

    val vendorEmail = raw.vendorEmail
    if ("support@yourcompany.com" == vendorEmail) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR_EMAIL, vendorEmail))
    }
    vendorEmail?.let { validatePropertyLength("vendor email", it, MAX_PROPERTY_LENGTH) }
  }

  private fun ValidationContext.validateSinceBuild(sinceBuild: String?) {
    if (sinceBuild == null) {
      registerProblem(SinceBuildNotSpecified(descriptorPath))
      return
    }
    val parsed = IdeVersion.createIdeVersionIfValid(sinceBuild)
    if (parsed == null) {
      registerProblem(InvalidSinceBuild(descriptorPath, sinceBuild))
      return
    }
    if (sinceBuild.endsWith(".*")) {
      registerProblem(SinceBuildCannotContainWildcard(descriptorPath, parsed))
    }
    if (parsed.productCode.isNotEmpty()) {
      registerProblem(ProductCodePrefixInBuild(descriptorPath))
    }
    // Shared with PluginSinceUntilRangeVerifier (the JAXB path) so both pipelines report identical
    // per-component range problems - see verifyIdeBuildComponentsRanges.
    verifyIdeBuildComponentsRanges(
      ideVersion = parsed,
      baselineLowerBound = SINCE_BASELINE_LOWER_BOUND,
      attributeName = IdeaVersionBean.SINCE_BUILD_ATTRIBUTE_NAME,
      descriptorPath = descriptorPath
    )
  }

  private fun ValidationContext.validateDependencies(dependencies: List<DependsElement>) {
    for (dependency in dependencies) {
      val id = dependency.pluginId
      if (id.isNullOrBlank() || id.contains("\n")) {
        registerProblem(InvalidDependencyId(descriptorPath, id))
      } else if (dependency.isOptional) {
        // Local val for the same cross-module smart-cast reason as validateVendor above.
        val configFile = dependency.configFile
        if (configFile == null) {
          registerProblem(OptionalDependencyConfigFileNotSpecified(id))
        } else if (configFile.isBlank()) {
          registerProblem(OptionalDependencyConfigFileIsEmpty(id, descriptorPath))
        }
      }
      // Note: old validateDependencies also flags SuperfluousNonOptionalDependencyDeclaration when
      // `optional == false` was EXPLICITLY present in XML (as opposed to simply absent/defaulted).
      // DependsElement.isOptional is a plain Boolean with no "was it explicit" bit, so that specific
      // check can't be ported 1:1 without changing the library's model - skipped for this POC.
    }
    // ReusedDescriptorVerifier (duplicate optional-dependency config-file detection) also skipped for POC.
  }

  private fun ValidationContext.validateModules(raw: RawPluginDescriptor) {
    if (raw.pluginAliases.any { it.isEmpty() }) {
      registerProblem(InvalidModuleBean(descriptorPath))
    }
  }
}
