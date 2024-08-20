/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.plugin

import com.jetbrains.plugin.structure.intellij.version.ProductReleaseVersion
import java.time.LocalDate

data class ProductDescriptor(
  val code: String,
  val releaseDate: LocalDate,
  val version: ProductReleaseVersion,
  val eap: Boolean,
  val optional: Boolean
) {

  @Deprecated("Use version", replaceWith = ReplaceWith("ProductDescriptor(code, releaseDate, version, eap, optional)"))
  constructor(
    code: String,
    releaseDate: LocalDate,
    releaseVersion: Int,
    eap: Boolean,
    optional: Boolean
  ) : this(
    code,
    releaseDate,
    ProductReleaseVersion(releaseVersion),
    eap,
    optional
  )

  @Deprecated("Use version.value field", replaceWith = ReplaceWith("version.value"))
  val releaseVersion: Int = version.value
}