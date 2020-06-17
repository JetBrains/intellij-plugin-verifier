/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu

import com.jetbrains.plugin.structure.base.plugin.PluginCreationFail
import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.plugin.Settings
import com.jetbrains.plugin.structure.base.problems.PluginFileSizeIsTooLarge
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.edu.bean.EduPluginDescriptor
import org.apache.commons.io.FileUtils
import java.io.File

internal fun validateEduPluginBean(manifest: EduPluginDescriptor): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()
  if (manifest.language.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("language"))
  }
  if (manifest.summary.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("summary"))
  }
  if (manifest.title.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("title"))
  }
  if (manifest.programmingLanguage.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("programming_language"))
  }
  if (manifest.courseVersion.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("version"))
  }
  return problems
}

fun validateEduPluginDirectory(pluginDirectory: File): PluginCreationFail<EduPlugin>? {
  val sizeLimit = Settings.EDU_PLUGIN_SIZE_LIMIT.getAsLong()
  if (FileUtils.sizeOfDirectory(pluginDirectory) > sizeLimit) {
    return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
  }
  return null
}