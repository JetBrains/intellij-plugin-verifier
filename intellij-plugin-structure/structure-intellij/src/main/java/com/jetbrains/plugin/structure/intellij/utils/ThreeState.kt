/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.utils

enum class ThreeState {
  YES, NO, UNSURE;

  companion object {
    @JvmStatic
    fun fromBoolean(value: Boolean) = if (value) YES else NO
  }
}