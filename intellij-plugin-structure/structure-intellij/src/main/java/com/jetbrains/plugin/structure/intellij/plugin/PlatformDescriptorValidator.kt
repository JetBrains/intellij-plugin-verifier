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
import com.jetbrains.plugin.structure.intellij.verifiers.PluginIdVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.ProductReleaseVersionVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.SINCE_BASELINE_LOWER_BOUND
import com.jetbrains.plugin.structure.intellij.verifiers.verifyIdeBuildComponentsRanges
import com.jetbrains.plugin.structure.intellij.verifiers.verifyUntilBuild
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jdom2.Document
import org.jdom2.Element
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val DEFAULT_TEMPLATE_NAMES = setOf("Plugin display name here", "My Framework Support", "Template", "Demo")

private val PLUGIN_NAME_RESTRICTED_WORDS = setOf(
  "plugin", "JetBrains", "IDEA", "PyCharm", "CLion", "AppCode", "DataGrip", "Fleet", "GoLand", "PhpStorm",
  "WebStorm", "Rider", "ReSharper", "TeamCity", "YouTrack", "RubyMine", "IntelliJ"
)

private const val MAX_VERSION_LENGTH = 64
private const val MAX_LONG_PROPERTY_LENGTH = 65535
private const val MAX_PRODUCT_CODE_LENGTH = 15

private const val PRODUCT_DESCRIPTOR_ELEMENT = "product-descriptor"

private val RELEASE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

/**
 * POC counterpart to [PluginBeanValidator], operating on [RawPluginDescriptor] (produced by
 * [PlatformPluginDescriptorParser]) instead of [com.jetbrains.plugin.structure.intellij.beans.PluginBean].
 *
 * The two validators are kept at parity check-for-check, and where a check needs nothing but a value
 * off the descriptor, they share one implementation rather than each having their own - see
 * [PluginIdVerifier], [ProductReleaseVersionVerifier], [verifyUntilBuild],
 * [verifyIdeBuildComponentsRanges], [validateDescriptionIsCorrect] and [validatePluginNameIsCorrect].
 * Parity matters more here than it looks: which pipeline a plugin gets is decided by
 * [PluginCreator.shouldUsePlatformParser], so any check living on one side only silently stops applying
 * to whichever plugins that rule sends the other way.
 *
 * Two checks cannot be reproduced from [RawPluginDescriptor] and are read off the descriptor XML instead
 * (see [validateProductDescriptor]): the library parses `release-date` into a `LocalDate` and
 * `release-version` into an `Int`, discarding the raw strings that `ReleaseDateWrongFormat` and
 * `ReleaseVersionWrongFormat` are about - a malformed date and a leading-zero `release-version` are
 * indistinguishable from valid ones once parsed.
 *
 * Known remaining gap: [RawPluginDescriptor] has no `eap` field at all, so
 * `<product-descriptor eap="...">` is neither validated nor carried through - see
 * [PlatformPluginDescriptorParser]'s class doc. `SuperfluousNonOptionalDependencyDeclaration` is also
 * absent, see [validateDependencies].
 */
internal class PlatformDescriptorValidator {
  private val pluginIdVerifier = PluginIdVerifier()
  private val productReleaseVersionVerifier = ProductReleaseVersionVerifier()

  /**
   * @param document the descriptor's own XML, needed for the `<product-descriptor>` attributes the
   *   library parses away - see [validateProductDescriptor].
   */
  fun validate(
    raw: RawPluginDescriptor,
    document: Document,
    validationContext: ValidationContext,
    validateDescriptor: Boolean
  ) {
    validationContext.validate(raw, document, validateDescriptor)
  }

  private fun ValidationContext.validate(
    raw: RawPluginDescriptor,
    document: Document,
    validateDescriptor: Boolean
  ) {
    if (validateDescriptor) {
      validateBeanUrl(raw.url)
      pluginIdVerifier.verify(raw.id, descriptorPath, ::registerProblem)
      validateName(raw.name)
      validateVersion(raw.version)
      validateDescription(raw.description)
      validateChangeNotes(raw.changeNotes)
      validateVendor(raw)
      validateSinceBuild(raw.sinceBuild)
      // Shared with PluginSinceUntilRangeVerifier (the JAXB path), see verifyUntilBuild: the selection
      // rule keys off `until-build`, so this pipeline is the one that sees the suspicious values.
      verifyUntilBuild(raw.untilBuild, descriptorPath)
      validateProductDescriptor(raw, document)
    }
    validateDependencies(raw.depends)
    validateModules(raw)
  }

  private fun ValidationContext.validateDescription(htmlDescription: String?) {
    validateDescriptionIsCorrect(
      propertyName = "description",
      descriptorPath = descriptorPath,
      htmlDescription = htmlDescription
    ).forEach { registerProblem(it) }
  }

  private fun ValidationContext.validateChangeNotes(changeNotes: String?) {
    // Requiring change-notes at all would be too strict - too many plugins omit them - so only a
    // change-notes that IS present is held to anything.
    if (changeNotes.isNullOrBlank()) return

    if (changeNotes.contains("Add change notes here") || changeNotes.contains("most HTML tags may be used")) {
      registerProblem(DefaultChangeNotes(descriptorPath))
    }
    validatePropertyLength("<change-notes>", changeNotes, MAX_LONG_PROPERTY_LENGTH)
  }

  /**
   * Validated against the descriptor XML rather than [raw], because [RawPluginDescriptor] keeps
   * `release-date` as a parsed `LocalDate` and `release-version` as an `Int`. Both of the problems in
   * question are about the *written form* - a date that does not parse as `yyyyMMdd`, a
   * `release-version` written with a leading zero - and neither survives that parse. The library also
   * has no `eap` field at all, so that attribute is still not covered.
   *
   * [document] is the unresolved descriptor, which is all this needs: `<product-descriptor>` is a direct
   * child of `<idea-plugin>` and is not something `<xi:include>` contributes.
   */
  private fun ValidationContext.validateProductDescriptor(raw: RawPluginDescriptor, document: Document) {
    val productDescriptor = document.rootElement.getChild(PRODUCT_DESCRIPTOR_ELEMENT) ?: return
    validateProductCode(raw.productCode)
    validateReleaseDate(productDescriptor.getAttributeValue("release-date"))
    productReleaseVersionVerifier.verify(
      productDescriptor.getAttributeValue("release-version"),
      raw.version,
      descriptorPath,
      ::registerProblem
    )
    validateBooleanFlag(productDescriptor, "optional")
  }

  private fun ValidationContext.validateProductCode(productCode: String?) {
    if (productCode.isNullOrEmpty()) {
      registerProblem(PropertyNotSpecified("code", descriptorPath))
    } else {
      validatePropertyLength("Product code", productCode, MAX_PRODUCT_CODE_LENGTH)
    }
  }

  private fun ValidationContext.validateReleaseDate(releaseDate: String?) {
    if (releaseDate.isNullOrEmpty()) {
      registerProblem(PropertyNotSpecified("release-date", descriptorPath))
      return
    }
    try {
      if (LocalDate.parse(releaseDate, RELEASE_DATE_FORMATTER) > LocalDate.now().plusDays(5)) {
        registerProblem(ReleaseDateInFuture(descriptorPath))
      }
    } catch (e: DateTimeParseException) {
      registerProblem(ReleaseDateWrongFormat(descriptorPath))
    }
  }

  private fun ValidationContext.validateBooleanFlag(element: Element, name: String) {
    val flag = element.getAttributeValue(name) ?: return
    if (flag != "true" && flag != "false") {
      registerProblem(NotBoolean(name, descriptorPath))
    }
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
        validatePluginNameIsCorrect(descriptorPath, name.trim())?.let { registerProblem(it) }
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
