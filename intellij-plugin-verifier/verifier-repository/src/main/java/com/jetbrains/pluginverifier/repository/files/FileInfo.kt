package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.resources.ResourceInfo
import java.nio.file.Path

/**
 * Descriptor of the file in the [file repository] [FileRepository].
 *
 * It consists of the [path to the file] [file] and its [size] [fileSize]
 */
data class FileInfo(val file: Path, val fileSize: SpaceAmount) : ResourceInfo<Path, SpaceWeight>(file, SpaceWeight(fileSize)) {
  override fun toString() = "File $file of size $fileSize"
}