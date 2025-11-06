/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.results.location.ClassLocation
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.InnerClassNode

interface ClassFile : ClassFileMember {
  override val location: ClassLocation
  override val containingClassFile: ClassFile

  val classFileOrigin: FileOrigin
  val name: String
  val packageName: String
  val javaPackageName: String
  val methods: Sequence<Method>
  val fields: Sequence<Field>
  val interfaces: List<String>
  val superName: String?
  val signature: String?
  val javaVersion: Int
  val enclosingClassName: String?

  override val annotations: List<AnnotationNode>
  val innerClasses: List<InnerClassNode>

  val isAbstract: Boolean
  val isFinal: Boolean
  val isInterface: Boolean
  val isStatic: Boolean
  val isEnum: Boolean

  override val isPublic: Boolean
  override val isProtected: Boolean
  override val isPrivate: Boolean
  override val isPackagePrivate: Boolean
  val isSuperFlag: Boolean
  override val isDeprecated: Boolean

  val nestHostClass: String?
}