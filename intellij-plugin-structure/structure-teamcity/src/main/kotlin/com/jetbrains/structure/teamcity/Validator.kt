package com.jetbrains.structure.teamcity

import com.jetbrains.structure.plugin.PluginProblem
import com.jetbrains.structure.problems.PropertyNotSpecified
import com.jetbrains.structure.teamcity.beans.TeamcityPluginBean

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
