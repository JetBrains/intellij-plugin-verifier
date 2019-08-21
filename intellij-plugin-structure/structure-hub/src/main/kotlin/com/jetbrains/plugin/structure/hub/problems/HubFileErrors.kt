package com.jetbrains.plugin.structure.hub.problems

import com.jetbrains.plugin.structure.base.problems.PluginFileError

class HubZipFileTooManyFilesError : PluginFileError() {
  override val message
    get() = "There are too many files in widget archive"
}