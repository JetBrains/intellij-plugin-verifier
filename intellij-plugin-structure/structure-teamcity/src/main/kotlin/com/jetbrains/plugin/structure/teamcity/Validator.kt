package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.teamcity.beans.TeamcityPluginBean
import com.jetbrains.plugin.structure.teamcity.problems.ForbiddenWordInPluginName

internal fun validateTeamcityPluginBean(bean: TeamcityPluginBean): List<PluginProblem> {
  val problems = arrayListOf<PluginProblem>()

  if (bean.name.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("name"))
  }

  val beanDisplayName = bean.displayName
  if (beanDisplayName == null || beanDisplayName.isBlank()) {
    problems.add(PropertyNotSpecified("display-name"))
  } else {
    val hasForbiddenWords = ForbiddenWordInPluginName.forbiddenWords.any { beanDisplayName.contains(it) }
    if (hasForbiddenWords) {
      problems.add(ForbiddenWordInPluginName)
    }
  }

  if (bean.version.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("version"))
  }
  if (bean.description.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("description"))
  }
  if (bean.vendor?.name.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("vendor name"))
  }
  if (bean.vendor?.url.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("vendor url"))
  }
  return problems
}
