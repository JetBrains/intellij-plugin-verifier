package com.jetbrains.pluginverifier.repository.files

import java.io.File

interface FileKeyMapper<K> {

  val directoriesStored: Boolean

  fun getFileNameWithoutExtension(key: K): String

  fun getKey(file: File): K?

}