/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.intellij.xinclude

import java.net.URL

data class XIncludeEntry(val documentPath: String, val documentUrl: URL) {
  override fun equals(other: Any?) = other is XIncludeEntry
    && documentUrl.toExternalForm() == other.documentUrl.toExternalForm()

  override fun hashCode() = documentUrl.toExternalForm().hashCode()
}