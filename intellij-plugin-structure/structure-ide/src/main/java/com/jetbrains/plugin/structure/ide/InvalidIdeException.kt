/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide

import java.nio.file.Path

/**
 * Indicates that a IDE instance can't be created because IDE residing by path is not a valid IDE.
 */
class InvalidIdeException : RuntimeException {
  val idePath: Path
  val reason: String

  constructor(idePath: Path, reason: String) : super("IDE by path '$idePath' is invalid: $reason") {
    this.idePath = idePath
    this.reason = reason
  }

  constructor(idePath: Path, cause: Exception) : super("IDE in path '$idePath' is invalid", cause) {
    this.idePath = idePath
    this.reason = cause.message ?: ""
  }
}