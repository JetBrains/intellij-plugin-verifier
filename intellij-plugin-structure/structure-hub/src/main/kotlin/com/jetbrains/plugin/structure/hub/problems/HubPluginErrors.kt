/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.hub.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.hub.HubPluginManager

class HubIconInvalidUrl(private val iconUrl: String?) : InvalidDescriptorProblem(HubPluginManager.DESCRIPTOR_NAME) {

  override val detailedMessage
    get() = "icon is not found by $iconUrl"

  override val level
    get() = Level.ERROR

}

class HubDependenciesNotSpecified : InvalidDescriptorProblem(HubPluginManager.DESCRIPTOR_NAME) {

  override val detailedMessage
    get() = "dependencies are not specified"

  override val level
    get() = Level.ERROR

}

class HubProductsNotSpecified : InvalidDescriptorProblem(HubPluginManager.DESCRIPTOR_NAME) {

  override val detailedMessage
    get() = "products are not specified"

  override val level
    get() = Level.ERROR

}
