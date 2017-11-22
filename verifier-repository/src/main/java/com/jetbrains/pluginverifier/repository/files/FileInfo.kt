package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import java.nio.file.Path

data class FileInfo(val file: Path, val size: SpaceAmount)