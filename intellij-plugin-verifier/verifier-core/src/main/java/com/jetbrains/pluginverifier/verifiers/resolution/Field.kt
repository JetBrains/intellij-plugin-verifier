package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.location.FieldLocation
import org.objectweb.asm.tree.AnnotationNode

interface Field : ClassFileMember {
  override val location: FieldLocation
  override val containingClassFile: ClassFile

  val name: String
  val descriptor: String
  val signature: String?
  override val runtimeInvisibleAnnotations: List<AnnotationNode>

  val isStatic: Boolean
  val isFinal: Boolean
  override val isDeprecated: Boolean

  override val isPublic: Boolean
  override val isProtected: Boolean
  override val isPrivate: Boolean
  override val isPackagePrivate: Boolean

  val initialValue: Any?
}