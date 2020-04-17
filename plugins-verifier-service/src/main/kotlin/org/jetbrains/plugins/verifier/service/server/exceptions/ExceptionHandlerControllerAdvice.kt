/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.plugins.verifier.service.server.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class ExceptionHandlerControllerAdvice {
  @ExceptionHandler(AuthenticationFailedException::class)
  @ResponseStatus(code = HttpStatus.FORBIDDEN, reason = "Failed to authenticate you")
  fun handleAuthenticationFailed(e: AuthenticationFailedException) = e.message

  @ExceptionHandler(NotFoundException::class)
  @ResponseStatus(code = HttpStatus.NOT_FOUND)
  fun handleAuthenticationFailed(e: NotFoundException) = e.message

  @ExceptionHandler(IllegalArgumentException::class)
  @ResponseStatus(code = HttpStatus.BAD_REQUEST)
  fun handleIllegalArgumentException(e: IllegalArgumentException) = e.message
}