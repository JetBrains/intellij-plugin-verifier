package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import com.jetbrains.pluginverifier.repository.downloader.SpaceWeight
import com.jetbrains.pluginverifier.repository.resources.ResourceInfo
import java.nio.file.Path

/**
 * Descriptor of the file in the [file repository] [FileRepository].
 *
 * It consists of the [path to the file] [file] and its [size] [fileSize], which can
 * be used to avoid unnecessary IO-requests.
 */
data class FileInfo(val file: Path, val fileSize: SpaceAmount) : ResourceInfo<Path>(file, SpaceWeight(fileSize))