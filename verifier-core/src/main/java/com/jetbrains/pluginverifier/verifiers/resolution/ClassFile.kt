package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.verifiers.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

class ClassFile(private val asmNode: ClassNode, val classFileOrigin: ClassFileOrigin) : ClassFileMember {

  override val location
    get() = ClassLocation(
        name,
        signature?.takeIf { it.isNotEmpty() },
        modifiers,
        classFileOrigin
    )

  override val containingClassFile
    get() = this

  val name: String
    get() = asmNode.name

  val packageName
    get() = name.substringBeforeLast('/', "")

  val javaPackageName
    get() = packageName.replace('/', '.')

  val isAbstract
    get() = asmNode.isAbstract()

  val isFinal
    get() = asmNode.isFinal()

  val isInterface
    get() = asmNode.isInterface()

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

  val methods: Sequence<Method>
    get() = asmNode.methods.asSequence().map { Method(this, it) }

  val fields: Sequence<Field>
    get() = asmNode.fields.asSequence().map { Field(this, it) }

  val interfaces: List<String>
    get() = asmNode.interfaces

  val superName: String?
    get() = asmNode.superName

  val signature: String?
    get() = asmNode.signature

  val accessType: AccessType
    get() = asmNode.access.getAccessType()

  val modifiers: Modifiers
    get() = Modifiers(asmNode.access)

  val isSuperFlag
    get() = asmNode.isSuperFlag()

  val javaVersion: Int
    get() = if (asmNode.version == Opcodes.V1_1) {
      1
    } else {
      asmNode.version - 44
    }

  val invisibleAnnotations: List<AnnotationNode>
    get() = asmNode.invisibleAnnotations.orEmpty()
}