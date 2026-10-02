/*
 * Copyright 2000-2025 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.intellij.problems.*
import com.jetbrains.plugin.structure.intellij.verifiers.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private const val MAX_LONG_PROPERTY_LENGTH = 65535
private const val MAX_VERSION_LENGTH = 64
private const val MAX_PRODUCT_CODE_LENGTH = 15

private val DEFAULT_TEMPLATE_NAMES = setOf("Plugin display name here", "My Framework Support", "Template", "Demo")

private val RELEASE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

private val PLUGIN_NAME_RESTRICTED_WORDS = setOf(
  "plugin", "JetBrains", "IDEA", "PyCharm", "CLion", "AppCode", "DataGrip", "Fleet", "GoLand", "PhpStorm",
  "WebStorm", "Rider", "ReSharper", "TeamCity", "YouTrack", "RubyMine", "IntelliJ"
)

/**
 * Validates a plugin descriptor through its [ValidatableDescriptor] view - [PluginBeanView] on the JAXB
 * path, [PlatformDescriptorView] on the platform-parser path. Both pipelines share this one validator so
 * that the pipeline chosen by [PluginCreator.shouldUsePlatformParser] cannot change which checks apply.
 */
class PluginDescriptorValidator {
  private val pluginIdVerifier = PluginIdVerifier()
  private val pluginSinceUntilRangeVerifier = PluginSinceUntilRangeVerifier()
  private val pluginProductReleaseVersionVerifier = ProductReleaseVersionVerifier()

  fun validate(descriptor: ValidatableDescriptor, validationContext: ValidationContext, validateDescriptor: Boolean) {
    validationContext.validate(descriptor, validateDescriptor)
  }

  private fun ValidationContext.validate(descriptor: ValidatableDescriptor, validateDescriptor: Boolean) {
    if (validateDescriptor) {
      validateUrl(descriptor.url)
      validateId(descriptor)
      validateName(descriptor.name)
      validateVersion(descriptor.version)
      validateDescription(descriptor.description)
      validateChangeNotes(descriptor.changeNotes)
      validateVendor(descriptor.vendor)
      pluginSinceUntilRangeVerifier.verify(descriptor, descriptorPath, ::registerProblem)
      validateProductDescriptor(descriptor)
    }
    validateDependencies(descriptor)
    validateModules(descriptor)
  }

  private fun ValidationContext.validatePropertyLength(propertyName: String, propertyValue: String, maxLength: Int) {
    if (propertyValue.length > maxLength) {
      registerProblem(TooLongPropertyValue(descriptorPath, propertyName, propertyValue.length, maxLength))
    }
  }

  private fun ValidationContext.validateId(descriptor: ValidatableDescriptor) {
    pluginIdVerifier.verify(descriptor, descriptorPath, ::registerProblem)
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
        validatePluginNameIsCorrect(descriptorPath, name.trim())?.let {
          registerProblem(it)
        }
      }
    }
  }

  private fun ValidationContext.validateUrl(url: String?) {
    if (url != null) {
      validatePropertyLength("plugin url", url, MAX_PROPERTY_LENGTH)
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
    validateDescriptionIsCorrect(
        propertyName = "description",
        descriptorPath = descriptorPath,
        htmlDescription = htmlDescription
    ).forEach {
      registerProblem(it)
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

  private fun ValidationContext.validateVendor(vendor: ValidatableDescriptor.VendorView?) {
    if (vendor == null) {
      registerProblem(PropertyNotSpecified("vendor", descriptorPath))
      return
    }

    val name = vendor.name
    if (name.isNullOrBlank()) {
      registerProblem(VendorCannotBeEmpty(descriptorPath))
      return
    }

    if ("YourCompany" == name) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR, name))
    }
    validatePropertyLength("vendor", name, MAX_PROPERTY_LENGTH)

    val url = vendor.url
    if ("https://www.yourcompany.com" == url) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR_URL, url))
    }
    url?.let { validatePropertyLength("vendor url", it, MAX_PROPERTY_LENGTH) }

    val email = vendor.email
    if ("support@yourcompany.com" == email) {
      registerProblem(PropertyWithDefaultValue(descriptorPath, PropertyWithDefaultValue.DefaultProperty.VENDOR_EMAIL, email))
    }
    email?.let { validatePropertyLength("vendor email", it, MAX_PROPERTY_LENGTH) }
  }

  private fun ValidationContext.validateProductDescriptor(descriptor: ValidatableDescriptor) {
    val productDescriptor = descriptor.productDescriptor ?: return
    validateProductCode(productDescriptor.code)
    validateReleaseDate(productDescriptor.releaseDate)
    pluginProductReleaseVersionVerifier.verify(descriptor, descriptorPath, ::registerProblem)
    productDescriptor.eap?.let { validateEapFlag(it) }
    productDescriptor.optional?.let { validateOptionalFlag(it) }
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

  private fun ValidationContext.validateDependencies(descriptor: ValidatableDescriptor) {
    for (dependency in descriptor.dependencies) {
      val id = dependency.pluginId
      val configFile = dependency.configFile
      if (id.isNullOrBlank() || id.contains("\n")) {
        registerProblem(InvalidDependencyId(descriptorPath, id.orEmpty()))
      } else if (dependency.optional == true) {
        if (configFile == null) {
          registerProblem(OptionalDependencyConfigFileNotSpecified(id))
        } else if (configFile.isBlank()) {
          registerProblem(OptionalDependencyConfigFileIsEmpty(id, descriptorPath))
        }
      } else if (dependency.optional == false) {
        registerProblem(SuperfluousNonOptionalDependencyDeclaration(id))
      }
    }
    ReusedDescriptorVerifier(descriptorPath).verify(descriptor, ::registerProblem)
  }

  private fun ValidationContext.validateModules(descriptor: ValidatableDescriptor) {
    if (descriptor.pluginAliases.any { it.isEmpty() }) {
      registerProblem(InvalidModuleBean(descriptorPath)) // TODO rename
    }
  }
}