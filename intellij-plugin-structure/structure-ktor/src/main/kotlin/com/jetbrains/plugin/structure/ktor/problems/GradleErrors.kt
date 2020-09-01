package com.jetbrains.plugin.structure.ktor.problems

import com.jetbrains.plugin.structure.base.problems.InvalidDescriptorProblem

class GradleRepoIncorrectDescription(val expectedField: String, val unexpectedField: String) : InvalidDescriptorProblem(null) {

  override val detailedMessage
    get() = "Gradle repository description is incorrect: expected only $expectedField to be set. " +
      "Please, delete $unexpectedField from the descriptor"

  override val level
    get() = Level.ERROR

}