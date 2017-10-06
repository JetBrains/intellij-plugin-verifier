package com.jetbrains.pluginverifier.results.problems

data class ClassPath(val type: Type, val path: String) {
  enum class Type { ROOT, CLASSES_DIRECTORY, JAR_FILE }
}