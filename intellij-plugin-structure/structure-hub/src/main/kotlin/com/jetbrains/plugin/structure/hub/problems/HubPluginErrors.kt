package com.jetbrains.plugin.structure.hub.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem

class HubDependenciesNotSpecified : InvalidDescriptorProblem("manifest.json") {

  override val detailedMessage
    get() = "dependencies are not specified"

  override val level
    get() = Level.ERROR

}

class HubProductsNotSpecified : InvalidDescriptorProblem("manifest.json") {

  override val detailedMessage
    get() = "products are not specified"

  override val level
    get() = Level.ERROR

}
