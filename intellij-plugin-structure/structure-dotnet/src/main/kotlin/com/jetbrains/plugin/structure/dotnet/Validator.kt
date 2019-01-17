package com.jetbrains.plugin.structure.dotnet

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.dotnet.beans.ReSharperPluginBean
import com.jetbrains.plugin.structure.dotnet.problems.InvalidIdError
import com.jetbrains.plugin.structure.dotnet.problems.InvalidVersionError

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

  val version = bean.version
  if (version == null || version.isBlank()) {
    problems.add(PropertyNotSpecified("version"))
  } else {
    try {
      NugetSemanticVersion.parse(version)
    } catch (e: IllegalArgumentException) {
      problems.add(InvalidVersionError(version))
    }
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
