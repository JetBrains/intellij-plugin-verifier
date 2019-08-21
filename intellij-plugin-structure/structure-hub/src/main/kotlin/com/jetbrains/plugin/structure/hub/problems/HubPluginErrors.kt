package com.jetbrains.plugin.structure.hub.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem
import com.jetbrains.plugin.structure.hub.HubPluginManager

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
