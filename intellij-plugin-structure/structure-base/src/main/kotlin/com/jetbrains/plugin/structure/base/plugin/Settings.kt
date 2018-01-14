package com.jetbrains.plugin.structure.base.plugin

import org.apache.commons.io.FileUtils
import java.io.File

enum class Settings(private val key: String, private val defaultValue: () -> String) {
  EXTRACT_DIRECTORY("intellij.structure.temp.dir", { File(FileUtils.getTempDirectory(), "extracted-plugins").absolutePath });

  fun get(): String = System.getProperty(key) ?: defaultValue()

  fun getAsFile(): File = File(get())
}