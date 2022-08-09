/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.problems

import com.jetbrains.plugin.structure.base.problems.IncorrectPluginFile
import com.jetbrains.plugin.structure.base.problems.PluginFileError

fun createIncorrectIntellijFileProblem(fileName: String): PluginFileError =
  IncorrectPluginFile(fileName, ".zip or .jar archive or a directory.")

class PluginZipIsEmpty : PluginFileError() {

  override val level
    get() = Level.ERROR

  override val message
    get() = "Plugin file is empty"

}

class PluginZipContainsUnknownFile(private val fileName: String) : PluginFileError() {

  override val message
    get() = "Plugin .zip file contains an unexpected file '$fileName'"

}

class PluginZipContainsSingleJarInRoot(private val fileName: String) : PluginFileError() {

  override val message
    get() = "Plugin zip file contains a single jar file in root '$fileName'."

}

class UnexpectedPluginZipStructure : PluginFileError() {

  override val message
    get() = "Unexpected plugin zip file structure. It should be <plugin_id>/lib/*.jar"

}

class PluginZipContainsMultipleFiles(private val fileNames: List<String>) : PluginFileError() {

  override val message
    get() = "Plugin root directory must not contain multiple files: ${fileNames.joinToString()}"

}

class UnableToReadPluginFile(private val reason: String) : PluginFileError() {

  override val message
    get() = "Invalid jar file: $reason"

}

class PluginLibDirectoryIsEmpty : PluginFileError() {

  override val message
    get() = "Directory 'lib' must not be empty"

}