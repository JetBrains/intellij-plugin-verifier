package com.jetbrains.plugin.structure.ide.layout

import java.nio.file.Path

fun interface ModuleClasspathProvider {
  fun getClasspath(moduleName: String): List<Path>
}

class LayoutComponentsClasspathProvider(private val layoutComponents: LayoutComponents) : ModuleClasspathProvider {
  override fun getClasspath(moduleName: String): List<Path> {
    return layoutComponents.find { it.name == moduleName && it.isClasspathable }?.getClasspaths()
      ?: emptyList()
  }
}