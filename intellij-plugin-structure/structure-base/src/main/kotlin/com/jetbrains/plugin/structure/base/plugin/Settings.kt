/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.base.plugin

import org.apache.commons.io.FileUtils
import java.nio.file.Path
import java.nio.file.Paths

enum class Settings(private val key: String, private val defaultValue: () -> String) {
  EXTRACT_DIRECTORY("intellij.structure.temp.dir", { Paths.get(FileUtils.getTempDirectory().absolutePath).resolve("extracted-plugins").toString() }),
  INTELLIJ_PLUGIN_SIZE_LIMIT("intellij.structure.intellij.plugin.size.limit", { FileUtils.ONE_GB.toString() }),
  FLEET_PLUGIN_SIZE_LIMIT("intellij.structure.fleet.plugin.size.limit", { FileUtils.ONE_GB.toString() }),
  TOOLBOX_PLUGIN_SIZE_LIMIT("intellij.structure.toolbox.plugin.size.limit", { FileUtils.ONE_GB.toString() }),
  TEAM_CITY_PLUGIN_SIZE_LIMIT("intellij.structure.team.city.plugin.size.limit", { FileUtils.ONE_GB.toString() }),
  TEAM_CITY_RECIPE_SIZE_LIMIT("intellij.structure.teamcity.recipe.size.limit", { FileUtils.ONE_MB.toString() }),
  RE_SHARPER_PLUGIN_SIZE_LIMIT("intellij.structure.re.sharper.plugin.size.limit", { FileUtils.ONE_GB.toString() }),
  HUB_PLUGIN_SIZE_LIMIT("intellij.structure.hub.plugin.size.limit", { (FileUtils.ONE_MB * 30).toString() }),
  HUB_PLUGIN_MAX_FILES_NUMBER("intellij.structure.hub.plugin.max.files.number", { 1000.toString() }),
  EDU_PLUGIN_SIZE_LIMIT("intellij.structure.edu.plugin.size.limit", { FileUtils.ONE_GB.toString() }),
  YOUTRACK_PLUGIN_SIZE_LIMIT("intellij.structure.youtrack.plugin.size.limit", { (100 * FileUtils.ONE_MB).toString() }),
  KTOR_FEATURE_SIZE_LIMIT("intellij.structure.edu.plugin.size.limit", { FileUtils.ONE_GB.toString() });

  fun get(): String = System.getProperty(key) ?: defaultValue()

  fun getAsPath(): Path = Paths.get(get())

  fun getAsLong(): Long = get().toLong()

  fun getAsInt(): Int = get().toInt()

  fun set(value: String) {
    System.setProperty(key, value)
  }
}