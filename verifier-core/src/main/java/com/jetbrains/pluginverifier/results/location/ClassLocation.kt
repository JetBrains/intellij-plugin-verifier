package com.jetbrains.pluginverifier.results.location

import com.jetbrains.pluginverifier.results.location.classpath.ClassPath
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.presentation.ClassGenericsSignatureOption
import com.jetbrains.pluginverifier.results.presentation.ClassOption
import com.jetbrains.pluginverifier.results.presentation.formatClassLocation
import java.util.*

data class ClassLocation(val className: String,
                         val signature: String,
                         val classPath: ClassPath,
                         val modifiers: Modifiers) : Location {

  override fun equals(other: Any?) = other is ClassLocation && className == other.className

  override fun hashCode() = Objects.hash(className)

  override fun toString() = formatClassLocation(ClassOption.FULL_NAME, ClassGenericsSignatureOption.WITH_GENERICS)
}

