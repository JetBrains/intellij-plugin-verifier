package com.jetbrains.plugin.structure.ide.layout

import com.jetbrains.plugin.structure.intellij.platform.LayoutComponent
import com.jetbrains.plugin.structure.intellij.platform.ProductInfo
import java.nio.file.Path

fun interface ModuleClasspathProvider {
  fun getClasspath(moduleName: String): List<Path>
}

class ProductInfoClasspathProvider(private val productInfo: ProductInfo) : ModuleClasspathProvider {
  override fun getClasspath(moduleName: String): List<Path> {
    return productInfo.layout
      .find { it.name == moduleName && it is LayoutComponent.Classpathable }
      ?.let { it as LayoutComponent.Classpathable }
      ?.getClasspath()
      ?: emptyList()
  }
}