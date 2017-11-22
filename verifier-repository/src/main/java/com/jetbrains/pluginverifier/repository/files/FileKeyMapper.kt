package com.jetbrains.pluginverifier.repository.files

import java.nio.file.Path

interface FileKeyMapper<K> {

  fun getFileNameWithoutExtension(key: K): String

  fun getKey(file: Path): K?

}