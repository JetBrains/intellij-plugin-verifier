/*
 * Copyright 2000-2021 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet

import com.jetbrains.plugin.structure.base.plugin.PluginProblem
import com.jetbrains.plugin.structure.base.problems.*
import com.jetbrains.plugin.structure.fleet.FleetPluginManager.Companion.DESCRIPTOR_NAME
import fleet.bundles.Barrel
import fleet.bundles.BundleSpec
import fleet.bundles.Coordinates
import fleet.bundles.KnownMeta

val NON_ID_SYMBOL_REGEX = "^[A-Za-z0-9_.]+$".toRegex()

fun validateFleetPluginBean(bundleSpec: BundleSpec): MutableList<PluginProblem> {
  val problems = mutableListOf<PluginProblem>()
  val readableName = bundleSpec.readableName
  if (readableName.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("name"))
  } else {
    validatePropertyLength(DESCRIPTOR_NAME, "name", readableName, MAX_NAME_LENGTH, problems)
  }
  if (!NON_ID_SYMBOL_REGEX.matches(bundleSpec.bundleId.name.name)) {
    problems.add(InvalidPluginIDProblem(bundleSpec.bundleId.name.name))
  }
  if (bundleSpec.description.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("description"))
  }
  if (bundleSpec.vendor.isNullOrBlank()) {
    problems.add(PropertyNotSpecified("vendor"))
  }
  if (bundleSpec.bundle.barrels.isNullOrEmpty()) {
    problems.add(PropertyNotSpecified("parts"))
  }
  val allFiles = bundleSpec.bundle.barrels.values.flatMap(Barrel::collectPaths)
  for (coord in allFiles) {
    if (coord !is Coordinates.Remote) {
      problems.add(NonRemoteCoordinate(coord))
    }
  }
  return problems
}

class NonRemoteCoordinate(
  private val file: Coordinates,
  descriptorPath: String? = null
) : InvalidDescriptorProblem(descriptorPath) {

  override val detailedMessage: String
    get() = "Only remote coordinates are allowed in plugin distribution: $file"

  override val level
    get() = Level.ERROR
}

fun Barrel?.collectPaths(): Collection<Coordinates> =
  if (this == null) emptyList() else (classPath + modulePath + squashedAutomaticModules.flatten())

val BundleSpec.readableName: String? get() = bundle.meta[KnownMeta.ReadableName]

val BundleSpec.description: String? get() = bundle.meta[KnownMeta.Description]

val BundleSpec.vendor: String? get() = bundle.meta[KnownMeta.Vendor]
