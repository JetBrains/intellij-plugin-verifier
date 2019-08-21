package com.jetbrains.plugin.structure.hub

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.hub.problems.HubDependenciesNotSpecified
import com.jetbrains.plugin.structure.hub.problems.HubProductsNotSpecified
import com.jetbrains.plugin.structure.hub.problems.HubZipFileTooManyFilesError
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import java.io.File

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

internal fun validateHubPluginBean(bean: HubPlugin): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()

  if (bean.pluginId.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("key"))
  }

  if (bean.pluginName.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("name"))
  }

  if (bean.vendor == null) {
    problems.add(PropertyNotSpecified("author"))
  }

  if (bean.description.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("description"))
  }

  val version = bean.pluginVersion
  if (version == null || version.isBlank()) {
    problems.add(PropertyNotSpecified("version"))
  }

  if (bean.dependencies == null) {
    problems.add(PropertyNotSpecified("dependencies"))
  } else if (bean.dependencies.isEmpty()) {
    problems.add(HubDependenciesNotSpecified())
  }

  if (bean.products == null) {
    problems.add(PropertyNotSpecified("products"))
  } else if (bean.products.isEmpty()) {
    problems.add(HubProductsNotSpecified())
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


fun validateHubPluginDirectory(pluginDirectory: File): PluginCreationFail<HubPlugin>? {
  val sizeLimit = Settings.HUB_PLUGIN_SIZE_LIMIT.getAsLong()
  if (FileUtils.sizeOfDirectory(pluginDirectory) > sizeLimit) {
    return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
  }
  val filesIterator = FileUtils.iterateFilesAndDirs(pluginDirectory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
  val maxHubFileNum = Settings.HUB_PLUGIN_MAX_FILES_NUMBER.getAsInt()
  if (filesIterator.asSequence().take(maxHubFileNum + 1).count() > maxHubFileNum) {
    return PluginCreationFail(HubZipFileTooManyFilesError())
  }
  return null
}