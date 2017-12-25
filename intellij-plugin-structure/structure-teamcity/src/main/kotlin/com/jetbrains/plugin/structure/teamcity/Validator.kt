package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.teamcity.beans.TeamcityPluginBean
import com.jetbrains.plugin.structure.teamcity.problems.ForbiddenWordInPluginName

internal fun validateTeamcityPluginBean(bean: TeamcityPluginBean): List<PluginProblem> {
  val problems = arrayListOf<PluginProblem>()
  val beanName = bean.name
  if (beanName == null || beanName.isBlank()) {
    problems.add(PropertyNotSpecified("name"))
  } else {
    val words = beanName.toLowerCase().split(' ')
    if (words.any { it in ForbiddenWordInPluginName.forbiddenWords }) {
      problems.add(ForbiddenWordInPluginName)
    }
  }

  if (bean.displayName.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("display-name"))
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
