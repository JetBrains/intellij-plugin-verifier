package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import java.nio.file.Path

/**
 * Descriptor of the file in the [file repository] [FileRepository].
 *
 * This consists of the [path to the file] [file] and its [size], which can
 * be used to avoid unnecessary IO-requests.
 */
data class FileInfo(val file: Path, val size: SpaceAmount)