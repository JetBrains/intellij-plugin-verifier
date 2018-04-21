package com.jetbrains.pluginverifier.results.hierarchy

import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileOrigin

data class ClassHierarchy(val name: String,
                          val isInterface: Boolean,
                          val classOrigin: ClassFileOrigin,
                          var superClass: ClassHierarchy?,
                          var superInterfaces: List<ClassHierarchy>)