/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.location.MethodLocation
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.TryCatchBlockNode

interface Method : ClassFileMember {
  override val location: MethodLocation
  override val containingClassFile: ClassFile

  val name: String
  val descriptor: String
  val signature: String?
  val exceptions: List<String>

  val isAbstract: Boolean
  val isStatic: Boolean
  val isFinal: Boolean
  override val isPublic: Boolean
  override val isProtected: Boolean
  override val isPrivate: Boolean
  override val isPackagePrivate: Boolean
  override val isDeprecated: Boolean
  val isConstructor: Boolean
  val isClassInitializer: Boolean
  val isVararg: Boolean
  val isNative: Boolean
  val isBridgeMethod: Boolean

  //ASM-specific classes are returned, to avoid mirroring of ASM classes. May be abstracted away of ASM, if necessary.
  val instructions: List<AbstractInsnNode>
  val tryCatchBlocks: List<TryCatchBlockNode>
  val localVariables: List<LocalVariableNode>
  val methodParameters: List<MethodParameter>
  override val runtimeInvisibleAnnotations: List<AnnotationNode>
}