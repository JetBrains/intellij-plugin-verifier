package com.jetbrains.plugin.structure.intellij.xinclude

import java.net.URL

data class XIncludeEntry(val documentPath: String, val documentUrl: URL) {
  override fun equals(other: Any?) = other is XIncludeEntry
      && documentUrl.toExternalForm() == other.documentUrl.toExternalForm()

  override fun hashCode() = documentUrl.toExternalForm().hashCode()
}