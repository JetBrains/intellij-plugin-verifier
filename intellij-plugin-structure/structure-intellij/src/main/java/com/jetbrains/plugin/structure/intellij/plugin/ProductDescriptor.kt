/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import java.time.LocalDate

data class ProductDescriptor(
  val code: String,
  val releaseDate: LocalDate,
  val releaseVersion: Int,
  val eap: Boolean,
  val optional: Boolean
)