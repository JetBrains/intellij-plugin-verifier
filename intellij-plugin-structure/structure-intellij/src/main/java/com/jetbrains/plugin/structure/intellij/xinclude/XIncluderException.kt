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
