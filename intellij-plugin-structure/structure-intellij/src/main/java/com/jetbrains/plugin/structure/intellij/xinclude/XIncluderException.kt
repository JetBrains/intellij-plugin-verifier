/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.xinclude

class XIncluderException : RuntimeException {

  constructor(path: List<XIncludeEntry>, message: String) : this(path, message, null)

  constructor(path: List<XIncludeEntry>, message: String, cause: Throwable?) : super(buildMessage(message, path), cause)

  override val message: String
    get() = super.message!!

  private companion object {
    fun buildMessage(message: String, path: List<XIncludeEntry>): String =
      message + " (at " + path.joinToString(separator = " -> ") { it.documentPath } + ")"
  }
}
