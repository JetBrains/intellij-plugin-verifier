package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.location.FieldLocation
import org.objectweb.asm.tree.AnnotationNode

interface Field : ClassFileMember {
  override val location: FieldLocation
  override val containingClassFile: ClassFile

  val name: String
  val descriptor: String
  val signature: String?
  val accessType: AccessType
  override val runtimeInvisibleAnnotations: List<AnnotationNode>

  val isStatic: Boolean
  val isFinal: Boolean
  override val isPublic: Boolean
  override val isProtected: Boolean
  override val isPrivate: Boolean
  override val isDefaultAccess: Boolean
  override val isDeprecated: Boolean
}