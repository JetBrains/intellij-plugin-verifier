package com.jetbrains.plugin.structure.youtrack.mock

fun getMockPluginFileContent(fileName: String): String {
  return object {}.javaClass.getResource("/youtrack/$fileName").readText()
}