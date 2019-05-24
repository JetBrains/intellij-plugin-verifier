package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.plugin.structure.base.utils.toList
import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.verifiers.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

class Method(val owner: ClassFile, private val asmNode: MethodNode) : ClassFileMember {

  override val location
    get() = MethodLocation(
        owner.location,
        name,
        descriptor,
        asmNode.getParameterNames(),
        signature?.takeIf { it.isNotEmpty() },
        modifiers
    )

  override val containingClassFile
    get() = owner

  val name: String
    get() = asmNode.name

  val descriptor: String
    get() = asmNode.desc

  val isAbstract
    get() = asmNode.isAbstract()

  val isStatic
    get() = asmNode.isStatic()

  val isFinal
    get() = asmNode.isFinal()

  val isPublic
    get() = asmNode.isPublic()

  val isProtected
    get() = asmNode.isProtected()

  val isPrivate
    get() = asmNode.isPrivate()

  val isDefaultAccess
    get() = asmNode.isDefaultAccess()

  val isDeprecated
    get() = asmNode.isDeprecated()

  val signature: String?
    get() = asmNode.signature

  val invisibleAnnotations: List<AnnotationNode>
    get() = asmNode.invisibleAnnotations.orEmpty()

  val localVariables: List<LocalVariableNode>
    get() = asmNode.localVariables.orEmpty()

  val isVararg
    get() = asmNode.access and Opcodes.ACC_VARARGS != 0

  val isNative
    get() = asmNode.access and Opcodes.ACC_NATIVE != 0

  val exceptions: List<String>
    get() = asmNode.exceptions.orEmpty()

  val tryCatchBlocks: List<TryCatchBlockNode>
    get() = asmNode.tryCatchBlocks.orEmpty()

  val instructions: List<AbstractInsnNode>
    get() = asmNode.instructions.iterator().toList()

  val isSynthetic
    get() = asmNode.access and Opcodes.ACC_SYNTHETIC != 0

  val isBridgeMethod
    get() = asmNode.access and Opcodes.ACC_BRIDGE != 0

  val accessType: AccessType
    get() = asmNode.access.getAccessType()

  val modifiers: Modifiers
    get() = Modifiers(asmNode.access)

}