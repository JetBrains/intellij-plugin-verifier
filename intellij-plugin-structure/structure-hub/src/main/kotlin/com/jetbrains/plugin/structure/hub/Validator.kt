package com.jetbrains.plugin.structure.hub

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.hub.problems.HubZipFileHasLargeFilesError
import com.jetbrains.plugin.structure.hub.problems.HubZipFileTooLargeError
import com.jetbrains.plugin.structure.hub.problems.HubZipFileTooManyFilesError
import java.util.zip.ZipFile

private const val MAX_HUB_ZIP_SIZE = 10 * 1024 * 1024
private const val MAX_HUB_FILE_SIZE = 30 * 1024 * 1024
private const val MAX_HUB_FILE_NUM = 1000

private val AUTHOR_REGEX = "^([^<(]+)\\s*(<[^>]+>)?\\s*(\\([^)]+\\))?\\s*$".toRegex()

internal fun validateHubPluginBean(bean: HubPlugin): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()

  if (bean.pluginId.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("id"))
  }

  if (bean.pluginName.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("name"))
  }

  if (bean.vendor == null) {
    // Missing author email in manifest.json. Getting vendor from author field.
    problems.add(PropertyNotSpecified("author"))
  }

  val version = bean.pluginVersion
  if (version == null || version.isBlank()) {
    problems.add(PropertyNotSpecified("version"))
  }

  if (bean.dependencies == null) {
    problems.add(PropertyNotSpecified("dependencies"))
  }
  if (bean.products == null) {
    problems.add(PropertyNotSpecified("products"))
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


fun validateHubZipFile(zipFile: ZipFile): PluginCreationFail<HubPlugin>? {
  if (zipFile.size() > MAX_HUB_ZIP_SIZE) {
    return PluginCreationFail(HubZipFileTooLargeError())
  }
  if (zipFile.entries().toList().any { entry -> entry.size > MAX_HUB_FILE_SIZE }) {
    return PluginCreationFail(HubZipFileHasLargeFilesError())
  }
  if (zipFile.entries().toList().size > MAX_HUB_FILE_NUM) {
    return PluginCreationFail(HubZipFileTooManyFilesError())
  }
  return null
}