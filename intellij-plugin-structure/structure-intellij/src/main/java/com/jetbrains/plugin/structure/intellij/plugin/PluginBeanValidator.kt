/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.problems.MAX_NAME_LENGTH
import com.jetbrains.plugin.structure.base.problems.NotBoolean
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.TooLongPropertyValue
import com.jetbrains.plugin.structure.base.problems.VendorCannotBeEmpty
import com.jetbrains.plugin.structure.base.problems.validatePluginNameIsCorrect
import com.jetbrains.plugin.structure.intellij.beans.IdeaVersionBean
import com.jetbrains.plugin.structure.intellij.beans.PluginBean
import com.jetbrains.plugin.structure.intellij.beans.PluginDependencyBean
import com.jetbrains.plugin.structure.intellij.beans.PluginVendorBean
import com.jetbrains.plugin.structure.intellij.beans.ProductDescriptorBean
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.verifiers.MAX_PROPERTY_LENGTH
import com.jetbrains.plugin.structure.intellij.verifiers.PluginIdVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.PluginUntilBuildVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.ProductReleaseVersionVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.ReusedDescriptorVerifier
import com.jetbrains.plugin.structure.intellij.verifiers.verifyNewlines
import com.jetbrains.plugin.structure.intellij.version.IdeVersion
import org.jsoup.Jsoup
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val MAX_LONG_PROPERTY_LENGTH = 65535
private const val MAX_VERSION_LENGTH = 64
private const val MAX_PRODUCT_CODE_LENGTH = 15

private val DEFAULT_TEMPLATE_NAMES = setOf("Plugin display name here", "My Framework Support", "Template", "Demo")
private val DEFAULT_TEMPLATE_DESCRIPTIONS = setOf(
  "Enter short description for your plugin here", "most HTML tags may be used", "example.com/my-framework"
)
// \u2013 - `–` (short dash) ans \u2014 - `—` (long dash)
@Suppress("RegExpSimplifiable")
private val STARTS_WITH_LATIN_SYMBOLS_REGEX = Regex("^[\\w\\s\\p{Punct}\\u2013\\u2014]{$MIN_DESCRIPTION_LENGTH,}")

private val RELEASE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

private val PLUGIN_NAME_RESTRICTED_WORDS = setOf(
  "plugin", "JetBrains", "IDEA", "PyCharm", "CLion", "AppCode", "DataGrip", "Fleet", "GoLand", "PhpStorm",
  "WebStorm", "Rider", "ReSharper", "TeamCity", "YouTrack", "RubyMine", "IntelliJ"
)

class PluginBeanValidator {
  private val pluginIdVerifier = PluginIdVerifier()
  private val pluginUntilBuildVerifier = PluginUntilBuildVerifier()
  private val pluginProductReleaseVersionVerifier = ProductReleaseVersionVerifier()

  fun validate(pluginBean: PluginBean, validationContext: ValidationContext, validateDescriptor: Boolean) {
    validationContext.validate(pluginBean, validateDescriptor)
  }

  private fun ValidationContext.validate(bean: PluginBean, validateDescriptor: Boolean) {
    if (validateDescriptor) {
      validateBeanUrl(bean.url)
      validateId(bean)
      validateName(bean.name)
      validateVersion(bean.pluginVersion)
      validateDescription(bean.description)
      validateChangeNotes(bean.changeNotes)
      validateVendor(bean.vendor)
      validateIdeaVersion(bean.ideaVersion)
      pluginUntilBuildVerifier.verify(bean, descriptorPath, ::registerProblem)
      validateProductDescriptor(bean, bean.productDescriptor)
    }
    validateDependencies(bean.dependencies)
    validateModules(bean)
  }

  private fun ValidationContext.validatePropertyLength(propertyName: String, propertyValue: String, maxLength: Int) {
    if (propertyValue.length > maxLength) {
      registerProblem(TooLongPropertyValue(descriptorPath, propertyName, propertyValue.length, maxLength))
    }
  }

  private fun ValidationContext.validateId(plugin: PluginBean) {
    pluginIdVerifier.verify(plugin, descriptorPath, ::registerProblem)
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
        verifyNewlines("name", name, descriptorPath, ::registerProblem)
        validatePluginNameIsCorrect(descriptorPath, name.trim())?.let {
          registerProblem(it)
        }
      }
    }
  }

  private fun ValidationContext.validateBeanUrl(beanUrl: String?) {
    if (beanUrl != null) {
      validatePropertyLength("plugin url", beanUrl, MAX_PROPERTY_LENGTH)
    }
  }

  private fun ValidationContext.validateVersion(pluginVersion: String?) {
    if (pluginVersion.isNullOrEmpty()) {
      registerProblem(PropertyNotSpecified("version", descriptorPath))
    } else {
      validatePropertyLength("version", pluginVersion, MAX_VERSION_LENGTH)
    }
  }

  private fun ValidationContext.validateDescription(htmlDescription: String?) {
    if (htmlDescription.isNullOrEmpty()) {
      registerProblem(PropertyNotSpecified("description", descriptorPath))
      return
    }
    validatePropertyLength("description", htmlDescription, MAX_LONG_PROPERTY_LENGTH)

    val html = Jsoup.parseBodyFragment(htmlDescription)
    val textDescription = html.text()

    if (DEFAULT_TEMPLATE_DESCRIPTIONS.any { textDescription.contains(it) }) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.DESCRIPTION, textDescription))
      return
    }

    val latinDescriptionPart = STARTS_WITH_LATIN_SYMBOLS_REGEX.find(textDescription)?.value
    if (latinDescriptionPart == null) {
      registerProblem(DescriptionNotStartingWithLatinCharacters())
    }
    val links = html.select("[href],img[src]")
    links.forEach { link ->
      val href = link.attr("abs:href")
      val src = link.attr("abs:src")
      if (href.startsWith("http://")) {
        registerProblem(HttpLinkInDescription(href))
      }
      if (src.startsWith("http://")) {
        registerProblem(HttpLinkInDescription(src))
      }
    }
  }

  private fun ValidationContext.validateChangeNotes(changeNotes: String?) {
    if (changeNotes.isNullOrBlank()) {
      //Too many plugins don't specify the change-notes, so it's too strict to require them.
      //But if specified, let's check that the change-notes are long enough.
      return
    }

    if (changeNotes.contains("Add change notes here") || changeNotes.contains("most HTML tags may be used")) {
      registerProblem(DefaultChangeNotes(descriptorPath))
    }
    validatePropertyLength("<change-notes>", changeNotes, MAX_LONG_PROPERTY_LENGTH)
  }

  private fun ValidationContext.validateVendor(vendorBean: PluginVendorBean?) {
    if (vendorBean == null) {
      registerProblem(PropertyNotSpecified("vendor", descriptorPath))
      return
    }

    if (vendorBean.name.isNullOrBlank()) {
      registerProblem(VendorCannotBeEmpty(descriptorPath))
      return
    }

    if ("YourCompany" == vendorBean.name) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR, vendorBean.name))
    }
    validatePropertyLength("vendor", vendorBean.name, MAX_PROPERTY_LENGTH)

    if ("https://www.yourcompany.com" == vendorBean.url) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR_URL, vendorBean.url))
    }
    validatePropertyLength("vendor url", vendorBean.url, MAX_PROPERTY_LENGTH)

    if ("support@yourcompany.com" == vendorBean.email) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR_EMAIL, vendorBean.email))
    }
    validatePropertyLength("vendor email", vendorBean.email, MAX_PROPERTY_LENGTH)
  }

  private fun ValidationContext.validateIdeaVersion(versionBean: IdeaVersionBean?) {
    if (versionBean == null) {
      registerProblem(PropertyNotSpecified("idea-version", descriptorPath))
      return
    }

    val sinceBuild = versionBean.sinceBuild
    validateSinceBuild(sinceBuild)
  }

  private fun ValidationContext.validateSinceBuild(sinceBuild: String?) {
    if (sinceBuild == null) {
      registerProblem(SinceBuildNotSpecified(descriptorPath))
    } else {
      val sinceBuildParsed = IdeVersion.createIdeVersionIfValid(sinceBuild)
      if (sinceBuildParsed == null) {
        registerProblem(InvalidSinceBuild(descriptorPath, sinceBuild))
      } else {
        if (sinceBuild.endsWith(".*")) {
          registerProblem(SinceBuildCannotContainWildcard(descriptorPath, sinceBuildParsed))
        }
        if (sinceBuildParsed.baselineVersion < 130) {
          registerProblem(InvalidSinceBuild(descriptorPath, sinceBuild))
        }
        if (sinceBuildParsed.baselineVersion > 999) {
          registerProblem(ErroneousSinceBuild(descriptorPath, sinceBuildParsed))
        }
        if (sinceBuildParsed.productCode.isNotEmpty()) {
          registerProblem(ProductCodePrefixInBuild(descriptorPath))
        }
      }
    }
  }

  private fun ValidationContext.validateProductDescriptor(plugin: PluginBean, productDescriptor: ProductDescriptorBean?) {
    if (productDescriptor != null) {
      validateProductCode(productDescriptor.code)
      validateReleaseDate(productDescriptor.releaseDate)
      pluginProductReleaseVersionVerifier.verify(plugin, descriptorPath, ::registerProblem)
      productDescriptor.eap?.let { validateEapFlag(it) }
      productDescriptor.optional?.let { validateOptionalFlag(it) }
    }
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
    } else {
      try {
        val date = LocalDate.parse(releaseDate, RELEASE_DATE_FORMATTER)
        if (date > LocalDate.now().plusDays(5)) {
          registerProblem(ReleaseDateInFuture(descriptorPath))
        }
      } catch (e: DateTimeParseException) {
        registerProblem(ReleaseDateWrongFormat(descriptorPath))
      }
    }
  }

  private fun ValidationContext.validateEapFlag(eapFlag: String) = validateBooleanFlag(eapFlag, "eap")

  private fun ValidationContext.validateOptionalFlag(optionalFlag: String) = validateBooleanFlag(optionalFlag, "optional")

  private fun ValidationContext.validateBooleanFlag(flag: String, name: String) {
    if (flag != "true" && flag != "false") {
      registerProblem(NotBoolean(name, descriptorPath))
    }
  }

  private fun ValidationContext.validateDependencies(dependencies: List<PluginDependencyBean>) {
    for (dependencyBean in dependencies) {
      if (dependencyBean.dependencyId.isNullOrBlank() || dependencyBean.dependencyId.contains("\n")) {
        registerProblem(InvalidDependencyId(descriptorPath, dependencyBean.dependencyId))
      } else if (dependencyBean.optional == true) {
        if (dependencyBean.configFile == null) {
          registerProblem(OptionalDependencyConfigFileNotSpecified(dependencyBean.dependencyId))
        } else if (dependencyBean.configFile.isBlank()) {
          registerProblem(OptionalDependencyConfigFileIsEmpty(dependencyBean.dependencyId, descriptorPath))
        }
      } else if (dependencyBean.optional == false) {
        registerProblem(SuperfluousNonOptionalDependencyDeclaration(dependencyBean.dependencyId))
      }
    }
    ReusedDescriptorVerifier(descriptorPath).verify(dependencies, ::registerProblem)
  }

  private fun ValidationContext.validateModules(bean: PluginBean) {
    if (bean.modules?.any { it.isEmpty() } == true) {
      registerProblem(InvalidModuleBean(descriptorPath))
    }
  }
}