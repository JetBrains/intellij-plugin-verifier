package com.jetbrains.pluginverifier.repository.files

import com.jetbrains.pluginverifier.repository.cleanup.SpaceAmount
import java.io.File

data class FileInfo(val file: File, val size: SpaceAmount)