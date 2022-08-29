/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.dotnet

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.MAX_DOT_NET_RELEASE_NOTES_LENGTH
import com.jetbrains.plugin.structure.base.problems.MAX_NAME_LENGTH
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.validatePropertyLength
import com.jetbrains.plugin.structure.dotnet.beans.ReSharperPluginBean
import com.jetbrains.plugin.structure.dotnet.problems.InvalidIdError
import com.jetbrains.plugin.structure.dotnet.problems.InvalidVersionError

internal fun validateDotNetPluginBean(bean: ReSharperPluginBean): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()

  val id = bean.id
  val description = bean.description

  if (id.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("id"))
  }

  if (bean.getAllDependencies().any { it.id == "Wave" }) {
    if (id != null && !id.contains('.')) {
      problems.add(InvalidIdError)
    }
  }

  val version = bean.version
  if (version.isNullOrBlank()) {
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
  val title = bean.title
  if (title != null) {
    validatePropertyLength("", "title", title, MAX_NAME_LENGTH, problems)
  }

  val releaseNotes = bean.changeNotes
  if (releaseNotes != null) {
    validatePropertyLength(
      descriptor = "",
      propertyName = "releaseNotes",
      propertyValue = releaseNotes,
      maxLength = MAX_DOT_NET_RELEASE_NOTES_LENGTH,
      problems = problems
    )
  }
  return problems
}
