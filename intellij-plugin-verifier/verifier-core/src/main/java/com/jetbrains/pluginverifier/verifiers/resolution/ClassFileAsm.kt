package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.verifiers.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

class ClassFileAsm(private val asmNode: ClassNode, override val classFileOrigin: ClassFileOrigin) : ClassFile {

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
    get() = asmNode.access.getAccessType()

  override val javaVersion
    get() = if (asmNode.version == Opcodes.V1_1) {
      1
    } else {
      asmNode.version - 44
    }

  override val runtimeInvisibleAnnotations
    get() = asmNode.invisibleAnnotations.orEmpty()


  override val isAbstract
    get() = asmNode.isAbstract()

  override val isFinal
    get() = asmNode.isFinal()

  override val isInterface
    get() = asmNode.isInterface()

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

  override val isSuperFlag
    get() = asmNode.isSuperFlag()

}