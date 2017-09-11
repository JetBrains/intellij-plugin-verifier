package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.teamcity.beans.TeamcityPluginBean

internal fun validateTeamcityPluginBean(bean: TeamcityPluginBean): List<PluginProblem>{
  val result = ArrayList<PluginProblem>()
  if(bean.name.isNullOrBlank()) {
    result.add(PropertyNotSpecified("name"))
  }
  if(bean.displayName.isNullOrBlank()) {
    result.add(PropertyNotSpecified("display-name"))
  }
  if(bean.version.isNullOrBlank()) {
    result.add(PropertyNotSpecified("version"))
  }
  return result
}
