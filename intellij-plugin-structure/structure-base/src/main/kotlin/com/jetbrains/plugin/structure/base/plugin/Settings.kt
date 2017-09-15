package com.jetbrains.plugin.structure.base.plugin

import org.apache.commons.io.FileUtils
import java.io.File

/**
 * @author Sergey Patrikeev
 */
enum class Settings(private val key: String,
                    private val defaultValue: (String) -> String) {
  EXTRACT_DIRECTORY("intellij.structure.temp.dir", { File(FileUtils.getTempDirectory(), "extracted-plugins").absolutePath });

  fun get(): String = System.getProperty(key) ?: defaultValue(key)

  fun getAsFile(): File = File(get())
}