package com.jetbrains.plugin.structure.base.contentBuilder

import com.jetbrains.plugin.structure.base.utils.createDir
import java.io.File

class FileSpec(private val content: ByteArray) : ContentSpec {
  override fun generate(target: File) {
    target.parentFile?.createDir()
    target.writeBytes(content)
  }
}