package com.jetbrains.pluginverifier.tests.repository

import com.jetbrains.pluginverifier.repository.files.FileNameMapper

class IntFileNameMapper : FileNameMapper<Int> {

  override fun getFileNameWithoutExtension(key: Int): String = key.toString()
}