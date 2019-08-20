package com.jetbrains.plugin.structure.hub

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.hub.problems.*
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import java.io.File

const val MAX_HUB_ZIP_SIZE = 10 * 1024 * 1024L
const val MAX_HUB_FILE_SIZE = 30 * 1024 * 1024L
const val MAX_HUB_FILE_NUM = 1000

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
    // Missing author email in manifest.json. Getting vendor from author field.
    problems.add(PropertyNotSpecified("author"))
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
  if (FileUtils.sizeOfDirectory(pluginDirectory) > MAX_HUB_FILE_SIZE) {
    return PluginCreationFail(HubZipFileTooLargeError())
  }
  val filesIterator = FileUtils.iterateFilesAndDirs(pluginDirectory, TrueFileFilter.TRUE, TrueFileFilter.TRUE)
  if (filesIterator.asSequence().take(MAX_HUB_FILE_NUM + 1).count() > MAX_HUB_FILE_NUM) {
    return PluginCreationFail(HubZipFileTooManyFilesError())
  }
  return null
}