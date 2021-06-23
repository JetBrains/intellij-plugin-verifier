/*
 * Copyright 2000-2021 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.MAX_NAME_LENGTH
import com.jetbrains.plugin.structure.base.problems.PropertyNotSpecified
import com.jetbrains.plugin.structure.base.problems.validatePropertyLength
import com.jetbrains.plugin.structure.fleet.FleetPluginManager.Companion.DESCRIPTOR_NAME
import com.jetbrains.plugin.structure.fleet.bean.FleetPluginDescriptor


fun validateFleetPluginBean(descriptor: FleetPluginDescriptor): List<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()
  if (descriptor.name.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("name"))
  }
  if (descriptor.id.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("id"))
  }
  if (descriptor.version.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("version"))
  }
  if (descriptor.entryPoint.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("entryPoint"))
  }
  if (descriptor.description.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("description"))
  }
  if (descriptor.vendor.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("vendor"))
  }
  if (descriptor.name != null) {
    validatePropertyLength(DESCRIPTOR_NAME, "name", descriptor.name!!, MAX_NAME_LENGTH, problems)
  }
  return problems
}
