package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.base.utils.toList
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.verifiers.getAccessType
import com.jetbrains.pluginverifier.verifiers.getParameterNames
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.LocalVariableNode
import org.objectweb.asm.tree.MethodNode

class MethodAsm(override val containingClassFile: ClassFile, private val asmNode: MethodNode) : Method {

  override val location
    get() = MethodLocation(
        containingClassFile.location,
        name,
        descriptor,
        asmNode.getParameterNames(),
        signature?.takeIf { it.isNotEmpty() },
        Modifiers(asmNode.access)
    )

  override val name: String
    get() = asmNode.name

  override val descriptor: String
    get() = asmNode.desc

  override val accessType
    get() = getAccessType(asmNode.access)

  override val signature: String?
    get() = asmNode.signature

  override val runtimeInvisibleAnnotations: List<AnnotationNode>
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
    get() = asmNode.access and Opcodes.ACC_ABSTRACT != 0

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

  override val isVararg
    get() = asmNode.access and Opcodes.ACC_VARARGS != 0

  override val isConstructor: Boolean
    get() = asmNode.name == "<init>"

  override val isClassInitializer: Boolean
    get() = asmNode.name == "<clinit>"

  override val isNative
    get() = asmNode.access and Opcodes.ACC_NATIVE != 0

  override val isSynthetic
    get() = asmNode.access and Opcodes.ACC_SYNTHETIC != 0

  override val isBridgeMethod
    get() = asmNode.access and Opcodes.ACC_BRIDGE != 0

}