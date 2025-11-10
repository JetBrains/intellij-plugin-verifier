/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.problems

import com.jetbrains.plugin.structure.base.utils.isDirectory
import com.jetbrains.plugin.structure.base.utils.isZip
import org.apache.commons.io.FileUtils
import java.nio.file.Path

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

class UnreadableZipOrJarFile(
  private val fileName: String,
  private val reason: String
) : PluginFileError() {
  override val message
    get() = "The plugin archive contains an unreadable or malformed file $fileName: $reason"

  companion object {
    fun of(zipOrJar: Path, throwable: Throwable) = UnreadableZipOrJarFile(
      zipOrJar.fileName.toString(),
      throwable.message ?: ""
    )
  }
}

class IncorrectJarOrDirectory(
  private val path: Path
) : PluginFileError() {
  override val message: String
    get() {
      val type = when {
        path.isZip() -> "ZIP file"
        path.isDirectory -> "directory"
        else -> "file"
      }
      return "The plugin artifact path must be a .jar archive or a directory, but was a $type at [$path]"
    }
}

class UnableToExtractZip() : PluginFileError() {
  private var additionalMessage: String? = null

  constructor(additionalMessage: String) : this() {
    this.additionalMessage = additionalMessage
  }

  override val message: String
    get() {
      val message = "The plugin archive file cannot be extracted"
      return if (additionalMessage != null) {
        "$message. $additionalMessage"
      } else {
        message
      }
    }
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
