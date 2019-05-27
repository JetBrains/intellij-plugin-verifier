package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.location.FieldLocation
import org.objectweb.asm.tree.AnnotationNode

interface Field : ClassFileMember {
  override val location: FieldLocation
  override val containingClassFile: ClassFile

  val owner: ClassFile
  val name: String
  val descriptor: String
  val signature: String?
  val accessType: AccessType
  val invisibleAnnotations: List<AnnotationNode>

  val isStatic: Boolean
  val isFinal: Boolean
  val isPublic: Boolean
  val isProtected: Boolean
  val isPrivate: Boolean
  val isDefaultAccess: Boolean
  val isDeprecated: Boolean
}