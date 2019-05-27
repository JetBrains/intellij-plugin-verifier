package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.base.utils.toList
import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.verifiers.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.MethodNode

class MethodAsm(override val owner: ClassFile, private val asmNode: MethodNode) : Method {

  override val location
    get() = MethodLocation(
        owner.location,
        name,
        descriptor,
        asmNode.getParameterNames(),
        signature?.takeIf { it.isNotEmpty() },
        Modifiers(asmNode.access)
    )

  override val containingClassFile
    get() = owner

  override val name: String
    get() = asmNode.name

  override val descriptor: String
    get() = asmNode.desc

  override val accessType: AccessType
    get() = asmNode.access.getAccessType()

  override val signature: String?
    get() = asmNode.signature

  override val invisibleAnnotations: List<AnnotationNode>
    get() = asmNode.invisibleAnnotations.orEmpty()

  override val localVariables: List<LocalVariableNode>
    get() = asmNode.localVariables.orEmpty()

  override val exceptions
    get() = asmNode.exceptions.orEmpty()

  override val tryCatchBlocks
    get() = asmNode.tryCatchBlocks.orEmpty()

  override val instructions: List<AbstractInsnNode>
    get() = asmNode.instructions.iterator().toList()


  override val isAbstract
    get() = asmNode.isAbstract()

  override val isStatic
    get() = asmNode.isStatic()

  override val isFinal
    get() = asmNode.isFinal()

  override val isPublic
    get() = asmNode.isPublic()

  override val isProtected
    get() = asmNode.isProtected()

  override val isPrivate
    get() = asmNode.isPrivate()

  override val isDefaultAccess
    get() = asmNode.isDefaultAccess()

  override val isDeprecated
    get() = asmNode.isDeprecated()

  override val isVararg
    get() = asmNode.access and Opcodes.ACC_VARARGS != 0

  override val isNative
    get() = asmNode.access and Opcodes.ACC_NATIVE != 0

  override val isSynthetic
    get() = asmNode.access and Opcodes.ACC_SYNTHETIC != 0

  override val isBridgeMethod
    get() = asmNode.access and Opcodes.ACC_BRIDGE != 0

}