package com.jetbrains.plugin.structure.base.plugin

data class PluginFile(
  val fileName: String,
  val content: ByteArray
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PluginFile

    if (fileName != other.fileName) return false
    if (!content.contentEquals(other.content)) return false

    return true
  }

  override fun hashCode(): Int {
    var result = fileName.hashCode()
    result = 31 * result + content.contentHashCode()
    return result
  }
}