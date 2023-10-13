/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.edu

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.MAX_NAME_LENGTH
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.validatePropertyLength
import com.jetbrains.plugin.structure.edu.EduPluginManager.Companion.DESCRIPTOR_NAME
import com.jetbrains.plugin.structure.edu.bean.EduPluginDescriptor
import com.jetbrains.plugin.structure.edu.problems.UnsupportedLanguage
import java.util.*

internal fun validateEduPluginBean(descriptor: EduPluginDescriptor): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()
  if (descriptor.title.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(TITLE))
  }
  if (descriptor.summary.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(SUMMARY))
  }
  if (descriptor.items.isEmpty()) {
    problems.add(PropertyNotSpecified(ITEMS))
  }
  if (descriptor.vendor?.name.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(VENDOR))
  }
  val version = descriptor.pluginVersion
  if (version.isNullOrEmpty()) {
    problems.add(PropertyNotSpecified(VERSION))
  }
  if (descriptor.descriptorVersion == null) {
    problems.add(PropertyNotSpecified(DESCRIPTOR_VERSION))
  }
  if (descriptor.title != null) {
    validatePropertyLength(DESCRIPTOR_NAME, TITLE, descriptor.title, MAX_NAME_LENGTH, problems)
  }
  validateLanguage(descriptor, problems)
  validateProgrammingLanguage(descriptor, problems)
  return problems
}

private fun validateProgrammingLanguage(descriptor: EduPluginDescriptor, problems: MutableList<PluginProblem>) {
  val programmingLanguage = descriptor.programmingLanguageId ?: descriptor.programmingLanguage

  if (programmingLanguage.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(PROGRAMMING_LANGUAGE_ID))
    return
  }
}

private fun validateLanguage(descriptor: EduPluginDescriptor, problems: MutableList<PluginProblem>) {
  if (descriptor.language.isNullOrBlank()) {
    problems.add(PropertyNotSpecified(LANGUAGE))
    return
  }
  if (Locale.getISOLanguages().find { it == descriptor.language } == null) {
    problems.add(UnsupportedLanguage(descriptor.language))
  }
}