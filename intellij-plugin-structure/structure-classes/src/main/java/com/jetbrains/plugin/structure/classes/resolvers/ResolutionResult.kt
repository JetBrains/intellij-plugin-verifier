package com.jetbrains.plugin.structure.classes.resolvers

sealed class ResolutionResult<out T> {

  object NotFound : ResolutionResult<Nothing>()

  data class Invalid(val message: String) : ResolutionResult<Nothing>()

  data class FailedToRead(val reason: String) : ResolutionResult<Nothing>()

  data class Found<T>(val value: T, val fileOrigin: FileOrigin) : ResolutionResult<T>()
}
