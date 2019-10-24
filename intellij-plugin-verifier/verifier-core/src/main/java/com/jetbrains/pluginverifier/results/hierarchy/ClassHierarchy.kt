package com.jetbrains.pluginverifier.results.hierarchy

data class ClassHierarchy(
  val name: String,
  val isInterface: Boolean,
  var superClass: ClassHierarchy?,
  var superInterfaces: List<ClassHierarchy>
)