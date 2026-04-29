/*
 * Copyright 2000-2024 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide.layout

enum class MissingLayoutFileMode {
  /**
   * Do not perform any validation of the layout component classpath.
   *
   * The IDE will be created anyway, but its structure might be incomplete or semantically incorrect.
   */
  IGNORE,

  /**
   * When any classpath element is missing, it will be skipped while all other valid classpath elements remain in use.
   * Additionally, warn about missing classpath elements in the log.
   */
  SKIP_MISSING_CLASSPATH_ELEMENTS,

  /**
   * When any classpath element is missing, skip all classpath elements in the layout component.
   * Don't log anything.
   */
  SKIP_SILENTLY,

  /**
   * When any classpath element is missing, skip all classpath elements in the layout component.
   * Warn about missing classpath elements in the log.
   */
  SKIP_AND_WARN,

  /**
   * When any classpath element is missing, throw an exception.
   *
   * It is up to the caller to handle the exception appropriately and decide whether
   * to construct the IDE model.
   */
  FAIL
}