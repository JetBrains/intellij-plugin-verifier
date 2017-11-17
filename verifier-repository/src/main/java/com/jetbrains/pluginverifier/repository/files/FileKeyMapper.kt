package com.jetbrains.pluginverifier.repository.files

import java.io.File

interface FileKeyMapper<K> {

  fun getFileNameWithoutExtension(key: K): String

  fun getKey(file: File): K?

}