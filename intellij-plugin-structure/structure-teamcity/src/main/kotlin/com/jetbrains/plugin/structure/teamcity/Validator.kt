/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.problems.PluginProblem
import com.jetbrains.plugin.structure.base.problems.MAX_NAME_LENGTH
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.validatePluginNameIsCorrect
import com.jetbrains.plugin.structure.base.problems.validatePropertyLength
import com.jetbrains.plugin.structure.teamcity.TeamcityPluginManager.Companion.DESCRIPTOR_NAME
import com.jetbrains.plugin.structure.teamcity.beans.TeamcityPluginBean
import com.jetbrains.plugin.structure.teamcity.problems.ForbiddenWordInPluginName

val PLUGIN_NAME_FORBIDDEN_WORDS = listOf("teamcity", "plugin")

internal fun validateTeamcityPluginBean(bean: TeamcityPluginBean): List<PluginProblem> {
  val problems = arrayListOf<PluginProblem>()

  if (bean.info?.name.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("name"))
  }

  val beanDisplayName = bean.info?.displayName
  if (beanDisplayName.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("display-name"))
  } else {
    validatePropertyLength(
        descriptor = DESCRIPTOR_NAME,
        propertyName = "display-name",
        propertyValue = beanDisplayName,
        maxLength = MAX_NAME_LENGTH,
        problems = problems
    )
    val hasForbiddenWords = PLUGIN_NAME_FORBIDDEN_WORDS.any { beanDisplayName.contains(it, ignoreCase = true) }
    if (hasForbiddenWords) {
      problems.add(ForbiddenWordInPluginName(PLUGIN_NAME_FORBIDDEN_WORDS))
    }
    validatePluginNameIsCorrect(descriptor = DESCRIPTOR_NAME, name = beanDisplayName, problems = problems)
  }
  val name = bean.info?.name
  if (name != null) {
    validatePropertyLength(DESCRIPTOR_NAME, "name", name, MAX_NAME_LENGTH, problems)
  }

  if (bean.info?.version.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("version"))
  }
  if (bean.info?.description.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("description"))
  }
  if (bean.info?.vendor?.name.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("vendor name"))
  }
  if (bean.info?.vendor?.url.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("vendor url"))
  }
  return problems
}
