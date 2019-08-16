package com.jetbrains.plugin.structure.hub.problems

import com.jetbrains.plugin.structure.base.problems.PluginDescriptorResolutionError

class HubZipFileTooManyFilesError : PluginDescriptorResolutionError() {
  override val message
    get() = "There are too many files in widget archive"
}

class HubZipFileHsLargeFilesError : PluginDescriptorResolutionError() {
  override val message
    get() = "Some of widget files are too large"
}

class HubZipFileTooLargeError : PluginDescriptorResolutionError() {
  override val message
    get() = "Widget archive size is larger than allowed"
}