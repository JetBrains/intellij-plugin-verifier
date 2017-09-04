package com.jetbrains.plugin.structure.teamcity

import com.jetbrains.plugin.structure.plugin.PluginProblem
import com.jetbrains.plugin.structure.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.teamcity.beans.TeamcityPluginBean

internal fun validateTeamcityPluginBean(bean: TeamcityPluginBean): List<PluginProblem>{
  val result = ArrayList<PluginProblem>()
  if(bean.name == null) {
    result.add(PropertyNotSpecified("name"))
  }
  if(bean.displayName == null) {
    result.add(PropertyNotSpecified("display-name"))
  }
  if(bean.version == null) {
    result.add(PropertyNotSpecified("version"))
  }
  return result
}
