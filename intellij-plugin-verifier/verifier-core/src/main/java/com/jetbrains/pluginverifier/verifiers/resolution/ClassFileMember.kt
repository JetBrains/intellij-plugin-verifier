package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.location.Location
import org.objectweb.asm.tree.AnnotationNode

interface ClassFileMember {
  val containingClassFile: ClassFile

  val location: Location

  val isDeprecated: Boolean

  val runtimeInvisibleAnnotations: List<AnnotationNode>
}