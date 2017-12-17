package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.downloader.SpaceWeight
import com.jetbrains.pluginverifier.repository.resources.ResourceInfo
import java.nio.file.Path

/**
 * Descriptor of the file in the [file repository] [FileRepository].
 *
 * This consists of the [path to the file] [file] and its [size], which can
 * be used to avoid unnecessary IO-requests.
 */
data class FileInfo(private val resourceInfo: ResourceInfo<Path>) {
  val file: Path
    get() = resourceInfo.resource

  val size: SpaceAmount
    get() = (resourceInfo.weight as SpaceWeight).spaceAmount
}