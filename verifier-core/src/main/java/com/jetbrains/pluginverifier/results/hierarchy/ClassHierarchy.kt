package com.jetbrains.pluginverifier.results.hierarchy

import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileOrigin
import java.io.Serializable

data class ClassHierarchy(
    val name: String,
    val isInterface: Boolean,
    val classOrigin: ClassFileOrigin,
    var superClass: ClassHierarchy?,
    var superInterfaces: List<ClassHierarchy>
) : Serializable {
  companion object {
    private const val serialVersionUID = 0L
  }
}