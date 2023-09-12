/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.PluginFileError

class PluginZipIsEmpty : PluginFileError() {
  override val level
    get() = Level.ERROR

  override val message
    get() = "Plugin file is empty. Ensure that the plugin file is not corrupted or incomplete."
}

class PluginZipContainsUnknownFile(private val fileName: String) : PluginFileError() {
  override val message
    get() = "The plugin archive file contains an unexpected file '$fileName'. It must contain a directory. " +
            "The plugin .jar file should be placed in the </lib> folder within the plugin's root directory, " +
            "along with all the required bundled libraries."
}

class PluginZipContainsSingleJarInRoot(private val fileName: String) : PluginFileError() {
  override val message
    get() = "The plugin archive file contains a single .jar file in the root folder '$fileName. The plugin .jar file " +
            "should be placed in the </lib> folder within the plugin's root directory, along with all the required " +
            "bundled libraries. "
}

class UnexpectedPluginZipStructure : PluginFileError() {
  override val message
    get() = "Unexpected plugin archive file structure. The plugin .jar file should be placed in the </lib> folder " +
            "within the plugin's root directory, along with all the required bundled libraries."
}

class PluginZipContainsMultipleFiles(private val fileNames: List<String>) : PluginFileError() {
  override val message
    get() = "The plugin root directory must not contain multiple files: ${fileNames.joinToString()}. The plugin .jar " +
            "file should be placed in the </lib> folder within the plugin's root directory, along with all the " +
            "required bundled libraries."
}

class UnableToReadPluginFile(private val reason: String) : PluginFileError() {
  override val message
    get() = "Unable to read the archive file: $reason."
}

class PluginLibDirectoryIsEmpty : PluginFileError() {
  override val message
    get() = "The <lib> directory must not be empty. Ensure that the libraries are defined correctly."
}