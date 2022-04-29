/*
 * Copyright 2000-2021 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.fleet.FleetPluginManager.Companion.DESCRIPTOR_NAME
import fleet.bundles.Barrel
import fleet.bundles.PluginDescriptor

val NON_ID_SYMBOL_REGEX = "^[A-Za-z0-9_.]+$".toRegex()

fun validateFleetPluginBean(descriptor: PluginDescriptor): MutableList<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()
  val readableName = descriptor.readableName
  if (readableName.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("name"))
  } else {
    validatePropertyLength(DESCRIPTOR_NAME, "name", readableName, MAX_NAME_LENGTH, problems)
  }
  if (!NON_ID_SYMBOL_REGEX.matches(descriptor.id.name)) {
    problems.add(InvalidPluginIDProblem(descriptor.id.name))
  }
  if (descriptor.description.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("description"))
  }
  if (descriptor.vendor.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("vendor"))
  }
  if (descriptor.frontend == null && descriptor.workspace == null) {
    problems.add(PropertyNotSpecified("parts"))
  }
  val allFiles = descriptor.frontend.collectPaths() + descriptor.workspace.collectPaths()
  for (coord in allFiles) {
    if (coord !is Barrel.Coordinates.Remote) {
      problems.add(NonRemoteCoordinate(coord))
    }
  }
  return problems
}

class NonRemoteCoordinate(
  private val file: Barrel.Coordinates,
  descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath) {

  override val detailedMessage: String
    get() = "Only remote coordinates are allowed in plugin distribution: $file"

  override val level
    get() = Level.ERROR
}

fun Barrel?.collectPaths(): Collection<Barrel.Coordinates> =
  if (this == null) emptyList() else (classPath + modulePath + squashedAutomaticModules.flatten())
