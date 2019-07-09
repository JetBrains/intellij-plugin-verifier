package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.ClassFileOrigin
import com.jetbrains.pluginverifier.results.location.ClassLocation
import org.objectweb.asm.tree.AnnotationNode

interface ClassFile : ClassFileMember {
  override val location: ClassLocation
  override val containingClassFile: ClassFile

  val classFileOrigin: ClassFileOrigin
  val name: String
  val packageName: String
  val javaPackageName: String
  val methods: Sequence<Method>
  val fields: Sequence<Field>
  val interfaces: List<String>
  val superName: String?
  val signature: String?
  val javaVersion: Int
  override val runtimeInvisibleAnnotations: List<AnnotationNode>

  val isAbstract: Boolean
  val isFinal: Boolean
  val isInterface: Boolean
  override val isPublic: Boolean
  override val isProtected: Boolean
  override val isPrivate: Boolean
  override val isPackagePrivate: Boolean
  val isSuperFlag: Boolean
  override val isDeprecated: Boolean
}