package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.verifiers.*
import org.objectweb.asm.tree.FieldNode

class FieldAsm(override val owner: ClassFile, private val asmNode: FieldNode) : Field {

  override val location
    get() = FieldLocation(
        owner.location,
        name,
        descriptor,
        signature?.takeIf { it.isNotEmpty() },
        Modifiers(asmNode.access)
    )

  override val containingClassFile
    get() = owner

  override val name: String
    get() = asmNode.name

  override val descriptor: String
    get() = asmNode.desc

  override val signature: String?
    get() = asmNode.signature

  override val accessType
    get() = asmNode.access.getAccessType()

  override val invisibleAnnotations
    get() = asmNode.invisibleAnnotations.orEmpty()


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

}