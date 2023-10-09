/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.problems

import org.apache.commons.io.FileUtils

abstract class PluginFileError : PluginProblem() {
  override val level
    get() = Level.ERROR
}

class IncorrectPluginFile(
  private val fileName: String,
  private val expectedFileType: String
) : PluginFileError() {
  override val message
    get() = "The plugin archive contains an unexpected file $fileName: it must be a $expectedFileType."
}

class IncorrectZipOrJarFile(
  private val fileName: String
) : PluginFileError() {
  override val message
    get() = "The plugin archive contains an unexpected file $fileName: it must be .zip, .jar or a directory."
}

class UnableToExtractZip : PluginFileError() {
  override val message
    get() = "The plugin archive file cannot be extracted."
}

class PluginFileSizeIsTooLarge(private val sizeLimit: Long) : PluginFileError() {
  override val message
    get() = "The plugin file size exceeds the maximum limit of ${FileUtils.byteCountToDisplaySize(sizeLimit)}: ensure " +
            "that the plugin file is compressed and meets the maximum size limit."
}

class TooManyFiles(private val filesLimit: Long) : PluginProblem() {
  override val message: String
    get() = "The plugin has more files than allowed: $filesLimit."

  override val level = Level.ERROR
}

class FileTooBig(val file: String, private val fileSizeLimit: Long) : PluginProblem() {
  override val message: String
    get() = "The file $file is bigger than max allowed size: $fileSizeLimit."

  override val level = Level.ERROR
}

class MissedFile(val file: String) : PluginProblem() {
  override val message: String
    get() = "The file $file is missed."

  override val level = Level.ERROR
}
