package com.jetbrains.plugin.structure.toolbox.mock

fun getMockPluginJsonContent(fileName: String): String {
  return object {}.javaClass.getResource("/toolbox/$fileName.json")!!.readText()
}
