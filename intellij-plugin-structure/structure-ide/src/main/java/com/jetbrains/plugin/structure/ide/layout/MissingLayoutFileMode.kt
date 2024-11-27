/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.layout

enum class MissingLayoutFileMode {
  IGNORE,
  SKIP_CLASSPATH,
  SKIP_SILENTLY,
  SKIP_AND_WARN,
  FAIL
}