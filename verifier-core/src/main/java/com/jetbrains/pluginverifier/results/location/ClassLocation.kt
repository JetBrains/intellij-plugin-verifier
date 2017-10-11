package com.jetbrains.pluginverifier.results.location

import com.jetbrains.pluginverifier.results.location.classpath.ClassPath
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.convertClassSignature
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.cutPackageConverter
import com.jetbrains.pluginverifier.results.presentation.PresentationUtils.normalConverter

data class ClassLocation(val className: String,
                         val signature: String,
                         val classPath: ClassPath,
                         val modifiers: Modifiers) : Location {
  override val shortPresentation: String = cutPackageConverter(className)

  override val fullPresentation: String = if (signature.isNotEmpty()) {
    normalConverter(className) + convertClassSignature(signature, cutPackageConverter)
  } else {
    normalConverter(className)
  }

  override fun toString(): String = fullPresentation
}

