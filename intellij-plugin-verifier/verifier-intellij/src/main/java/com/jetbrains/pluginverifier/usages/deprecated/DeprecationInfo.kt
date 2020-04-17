/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.usages.deprecated

/**
 * Contains additional information about deprecated API:
 * - [forRemoval] - whether the deprecated API is scheduled for removal
 * - [untilVersion] - version of the product where API is to be removed
 * (makes sense only if [forRemoval] is true)
 */
data class DeprecationInfo(
  val forRemoval: Boolean,
  val untilVersion: String?
)