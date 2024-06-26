/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.classes.resolvers.FileOrigin
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.verifiers.getAccessType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InnerClassNode

class ClassFileAsm(private val asmNode: ClassNode, override val classFileOrigin: FileOrigin) : ClassFile {
  override val location
    get() = ClassLocation(
      name,
      signature?.takeIf { it.isNotEmpty() },
      Modifiers(asmNode.access),
      classFileOrigin
    )

  override val containingClassFile
    get() = this

  override val name: String
    get() = asmNode.name

  override val packageName
    get() = name.substringBeforeLast('/', "")

  override val javaPackageName
    get() = packageName.replace('/', '.')

  override val methods
    get() = asmNode.methods.asSequence().map { MethodAsm(this, it) }

  override val fields
    get() = asmNode.fields.asSequence().map { FieldAsm(this, it) }

  override val interfaces
    get() = asmNode.interfaces.orEmpty()

  override val superName: String?
    get() = asmNode.superName

  override val signature: String?
    get() = asmNode.signature

  override val accessType
    get() = getAccessType(asmNode.access)

  override val javaVersion
    get() = if (asmNode.version == Opcodes.V1_1) {
      1
    } else {
      asmNode.version - 44
    }

  override val enclosingClassName: String?
    get() {
      val outerClass = asmNode.outerClass
      if (outerClass != null) {
        return outerClass
      }
      return asmNode.innerClasses.find { it.name == name }?.outerName
    }

  override val innerClasses: List<InnerClassNode>
    get() {
      return asmNode.innerClasses
    }

  override val annotations
    get() = asmNode.invisibleAnnotations.orEmpty() + asmNode.visibleAnnotations.orEmpty()

  override val isAbstract
    get() = asmNode.access and Opcodes.ACC_ABSTRACT != 0

  override val isFinal
    get() = asmNode.access and Opcodes.ACC_FINAL != 0

  override val isInterface
    get() = asmNode.access and Opcodes.ACC_INTERFACE != 0

  override val isPublic
    get() = asmNode.access and Opcodes.ACC_PUBLIC != 0

  override val isProtected
    get() = asmNode.access and Opcodes.ACC_PROTECTED != 0

  override val isPrivate
    get() = asmNode.access and Opcodes.ACC_PRIVATE != 0

  override val isPackagePrivate
    get() = (asmNode.access and Opcodes.ACC_PUBLIC == 0) && (asmNode.access and Opcodes.ACC_PRIVATE == 0) && (asmNode.access and Opcodes.ACC_PROTECTED == 0)

  override val isDeprecated
    get() = asmNode.access and Opcodes.ACC_DEPRECATED != 0

  override val isSuperFlag
    get() = asmNode.access and Opcodes.ACC_SUPER != 0

  override val isSynthetic
    get() = asmNode.access and Opcodes.ACC_SYNTHETIC != 0

  override val isStatic
    get() = asmNode.access and Opcodes.ACC_STATIC != 0

  override val nestHostClass: String?
    get() = asmNode.nestHostClass
}