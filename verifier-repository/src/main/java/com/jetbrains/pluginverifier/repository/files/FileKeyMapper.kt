package com.jetbrains.pluginverifier.repository.files

interface FileKeyMapper<K> {

  fun getFileNameWithoutExtension(key: K): String

}