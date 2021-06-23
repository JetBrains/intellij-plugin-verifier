/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.hub

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.problems.MAX_NAME_LENGTH
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.validatePropertyLength
import com.jetbrains.plugin.structure.hub.HubPluginManager.Companion.DESCRIPTOR_NAME
import com.jetbrains.plugin.structure.hub.bean.HubPluginManifest
import com.jetbrains.plugin.structure.hub.problems.HubDependenciesNotSpecified
import com.jetbrains.plugin.structure.hub.problems.HubProductsNotSpecified
import com.jetbrains.plugin.structure.hub.problems.HubZipFileTooManyFilesError
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

/**
 * Trustworthy regexp copied from https://github.com/jonschlinkert/parse-author
 * It may be used to parse the following author formats:
 * ```
 * Name
 * Name <email>
 * Name <email> (url)
 * <email> (url)
 * ... and similar ...
 * ```
 */
private val AUTHOR_REGEX = "^([^<(]+)\\s*(<[^>]+>)?\\s*(\\([^)]+\\))?\\s*$".toRegex()

internal fun validateHubPluginBean(manifest: HubPluginManifest): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()

  if (manifest.pluginId.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("key"))
  }

  if (manifest.pluginName.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("name"))
  }

  if (manifest.author == null) {
    problems.add(PropertyNotSpecified("author"))
  }

  if (manifest.description.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("description"))
  }

  val version = manifest.pluginVersion
  if (version == null || version.isBlank()) {
    problems.add(PropertyNotSpecified("version"))
  }

  if (manifest.dependencies == null) {
    problems.add(PropertyNotSpecified("dependencies"))
  } else if (manifest.dependencies.isEmpty()) {
    problems.add(HubDependenciesNotSpecified())
  }

  if (manifest.products == null) {
    problems.add(PropertyNotSpecified("products"))
  } else if (manifest.products.isEmpty()) {
    problems.add(HubProductsNotSpecified())
  }
  if (manifest.pluginName != null) {
    validatePropertyLength(DESCRIPTOR_NAME, "name", manifest.pluginName, MAX_NAME_LENGTH, problems)
  }
  return problems
}

fun parseHubVendorInfo(author: String): VendorInfo {
  val authorMatch = AUTHOR_REGEX.find(author) ?: return VendorInfo()
  val vendorObject = authorMatch.groups
  val vendor = vendorObject[1]?.value?.trim()
  var vendorEmail = authorMatch.groups[2]?.value
  vendorEmail = vendorEmail?.substring(1, vendorEmail.length - 1) ?: ""
  var vendorUrl = authorMatch.groups[3]?.value
  vendorUrl = vendorUrl?.substring(1, vendorUrl.length - 1) ?: ""
  return VendorInfo(vendor, vendorEmail, vendorUrl)
}


fun validateHubPluginDirectory(pluginDirectory: Path): PluginCreationFail<HubPlugin>? {
  Files.walk(pluginDirectory).use { filesIterator ->
    val maxHubFileNum = Settings.HUB_PLUGIN_MAX_FILES_NUMBER.getAsInt()
    if (filesIterator.asSequence().take(maxHubFileNum + 1).count() > maxHubFileNum) {
      return PluginCreationFail(HubZipFileTooManyFilesError())
    }
    return null
  }
}