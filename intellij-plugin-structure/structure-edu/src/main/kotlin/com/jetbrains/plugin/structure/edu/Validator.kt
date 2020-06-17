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
import com.jetbrains.plugin.structure.edu.problems.UnsupportedLanguage
import com.jetbrains.plugin.structure.edu.problems.UnsupportedProgrammingLanguage
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*

internal fun validateEduPluginBean(descriptor: EduPluginDescriptor): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()
  if (descriptor.title.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(TITLE))
  }
  if (descriptor.summary.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(SUMMARY))
  }
  if (descriptor.language.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(LANGUAGE))
  }
  if (descriptor.programmingLanguage.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(PROGRAMMING_LANGUAGE))
  }
  if (Locale.getISOLanguages().find { displayLanguageByCode(it) == descriptor.language } == null) {
    problems.add(UnsupportedLanguage)
  }
  if (descriptor.programmingLanguage !in UnsupportedProgrammingLanguage.supportedLanguages) {
    problems.add(UnsupportedProgrammingLanguage)
  }
  if (descriptor.items == null || descriptor.items.isEmpty()) {
    problems.add(PropertyNotSpecified(ITEMS))
  }

  // TODO: plugin version format
  return problems
}

private fun displayLanguageByCode(languageCode: String) = Locale(languageCode).getDisplayLanguage(Locale.ENGLISH)

fun validateEduPluginDirectory(pluginDirectory: File): PluginCreationFail<EduPlugin>? {
  val sizeLimit = Settings.EDU_PLUGIN_SIZE_LIMIT.getAsLong()
  if (FileUtils.sizeOfDirectory(pluginDirectory) > sizeLimit) {
    return PluginCreationFail(PluginFileSizeIsTooLarge(sizeLimit))
  }
  return null
}