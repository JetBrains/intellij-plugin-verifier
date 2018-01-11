package com.jetbrains.plugin.structure.dotnet

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.dotnet.beans.ReSharperPluginBean
import com.jetbrains.plugin.structure.dotnet.problems.InvalidIdError

internal fun validateDotNetPluginBean(bean: ReSharperPluginBean): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()

  val dependencies = bean.dependencies
  val id = bean.id
  val description = bean.description

  if (id.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("id"))
  }

  if (dependencies != null && dependencies.any { it.id == "Wave" }) {
    if (id != null && id.count { it == '.' } != 1) {
      problems.add(InvalidIdError)
    }
  }

  if (bean.version.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("version"))
  }

  if (bean.authors.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("authors"))
  }

  if (description.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("description"))
  }

  if (bean.licenseUrl.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("licenseUrl"))
  }

  return problems
}
