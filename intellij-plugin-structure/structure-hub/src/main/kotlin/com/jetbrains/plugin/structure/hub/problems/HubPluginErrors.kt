/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.hub.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.hub.HubPluginManager

class HubIconInvalidUrl(iconUrl: String?) : InvalidDescriptorProblem(
  descriptorPath = HubPluginManager.DESCRIPTOR_NAME,
  detailedMessage = "The plugin icon is not found by $iconUrl."
) {
  override val level
    get() = Level.ERROR

}

class HubDependenciesNotSpecified : InvalidDescriptorProblem(
  descriptorPath = HubPluginManager.DESCRIPTOR_NAME,
  detailedMessage = "Widget dependencies are not specified."
) {
  override val level
    get() = Level.ERROR

}

class HubProductsNotSpecified : InvalidDescriptorProblem(
  descriptorPath = HubPluginManager.DESCRIPTOR_NAME,
  detailedMessage = "Products compatibility is not specified."
) {
  override val level
    get() = Level.ERROR

}
