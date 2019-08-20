package com.jetbrains.plugin.structure.base.plugin

import org.apache.commons.io.FileUtils
import java.io.File

enum class Settings(private val key: String, private val defaultValue: () -> String) {
  EXTRACT_DIRECTORY("intellij.structure.temp.dir", { File(FileUtils.getTempDirectory(), "extracted-plugins").absolutePath }),
  INTELLIJ_PLUGIN_SIZE_LIMIT("intellij.structure.intellij.plugin.size.limit", { FileUtils.ONE_GB.toString() }),
  TEAM_CITY_PLUGIN_SIZE_LIMIT("intellij.structure.team.city.plugin.size.limit", { FileUtils.ONE_GB.toString() }),
  RE_SHARPER_PLUGIN_SIZE_LIMIT("intellij.structure.re.sharper.plugin.size.limit", { FileUtils.ONE_GB.toString() }),
  HUB_PLUGIN_SIZE_LIMIT("intellij.structure.hub.plugin.size.limit", { (FileUtils.ONE_MB * 30).toString() }),
  HUB_PLUGIN_MAX_FILES_NUMBER("intellij.structure.hub.plugin.max.files.number", { 1000.toString() });

  fun get(): String = System.getProperty(key) ?: defaultValue()

  fun getAsFile(): File = File(get())

  fun getAsLong(): Long = get().toLong()

  fun getAsInt(): Int = get().toInt()
}