/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.classes.resolvers

sealed class ResolutionResult<out T> {

  object NotFound : ResolutionResult<Nothing>()

  data class Invalid(val message: String) : ResolutionResult<Nothing>()

  data class FailedToRead(val reason: String) : ResolutionResult<Nothing>()

  data class Found<T>(val value: T, val fileOrigin: FileOrigin) : ResolutionResult<T>()
}
