package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.verifiers.getAccessType
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.FieldNode

class FieldAsm(override val containingClassFile: ClassFile, private val asmNode: FieldNode) : Field {
  override val location
    get() = FieldLocation(
        containingClassFile.location,
        name,
        descriptor,
        signature?.takeIf { it.isNotEmpty() },
        Modifiers(asmNode.access)
    )

  override val name: String
    get() = asmNode.name

  override val descriptor: String
    get() = asmNode.desc

  override val signature: String?
    get() = asmNode.signature

  override val accessType
    get() = getAccessType(asmNode.access)

  override val runtimeInvisibleAnnotations
    get() = asmNode.invisibleAnnotations.orEmpty()

  override val isStatic
    get() = asmNode.access and Opcodes.ACC_STATIC != 0

  override val isFinal
    get() = asmNode.access and Opcodes.ACC_FINAL != 0

  override val isPublic
    get() = asmNode.access and Opcodes.ACC_PUBLIC != 0

  override val isProtected
    get() = asmNode.access and Opcodes.ACC_PROTECTED != 0

  override val isPrivate
    get() = asmNode.access and Opcodes.ACC_PRIVATE != 0

  override val isPackagePrivate
    get() = (asmNode.access and Opcodes.ACC_PUBLIC == 0) && (asmNode.access and Opcodes.ACC_PROTECTED == 0) && (asmNode.access and Opcodes.ACC_PRIVATE == 0)

  override val isDeprecated
    get() = asmNode.access and Opcodes.ACC_DEPRECATED != 0

  override val isSynthetic: Boolean
    get() = asmNode.access and Opcodes.ACC_SYNTHETIC != 0

  override val initialValue: Any?
    get() = asmNode.value
}