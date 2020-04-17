/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.teamcity.beans.TeamcityPluginBean
import com.jetbrains.plugin.structure.teamcity.problems.ForbiddenWordInPluginName

internal fun validateTeamcityPluginBean(bean: TeamcityPluginBean): List<PluginProblem> {
  val problems = arrayListOf<PluginProblem>()

  if (bean.info?.name.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("name"))
  }

  val beanDisplayName = bean.info?.displayName
  if (beanDisplayName == null || beanDisplayName.isBlank()) {
    problems.add(PropertyNotSpecified("display-name"))
  } else {
    val hasForbiddenWords = ForbiddenWordInPluginName.forbiddenWords.any { beanDisplayName.contains(it, ignoreCase = true) }
    if (hasForbiddenWords) {
      problems.add(ForbiddenWordInPluginName)
    }
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
