/*
 * Copyright 2000-2021 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.fleet.bean

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetPluginDescriptor(
  val id: String? = null,
  val name: String? = null,
  val version: String? = null,
  val description: String? = null,
  val entryPoint: String? = null,
  val requires: List<FleetDependency>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetDependency(val id: String, val version: FleetDependencyVersion)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FleetDependencyVersion(val min: String, val max: String)