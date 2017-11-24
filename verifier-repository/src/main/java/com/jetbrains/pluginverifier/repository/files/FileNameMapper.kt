package com.jetbrains.pluginverifier.repository.files

interface FileNameMapper<K> {

  fun getFileNameWithoutExtension(key: K): String

}