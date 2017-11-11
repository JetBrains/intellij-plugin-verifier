package com.jetbrains.pluginverifier.results.hierarchy

data class ClassHierarchy(val name: String,
                          val isInterface: Boolean,
                          val isIdeClass: Boolean,
                          var superClass: ClassHierarchy?,
                          var superInterfaces: List<ClassHierarchy>)