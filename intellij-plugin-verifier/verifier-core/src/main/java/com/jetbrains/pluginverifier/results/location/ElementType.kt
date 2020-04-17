/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.results.location

/**
 * Type of an API element.
 */
enum class ElementType(val presentableName: String) {
  CLASS("class"),
  INTERFACE("interface"),
  ANNOTATION("annotation"),
  ENUM("enum"),
  METHOD("method"),
  CONSTRUCTOR("constructor"),
  FIELD("field");

  override fun toString() = presentableName
}