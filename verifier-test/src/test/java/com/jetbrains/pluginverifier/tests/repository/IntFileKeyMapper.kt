package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.files.FileKeyMapper

class IntFileKeyMapper : FileKeyMapper<Int> {

  override fun getFileNameWithoutExtension(key: Int): String = key.toString()
}