package com.jetbrains.pluginverifier.verifiers.resolution

import com.jetbrains.pluginverifier.results.access.AccessType
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.verifiers.*
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.FieldNode

class Field(val owner: ClassFile, private val asmNode: FieldNode) : ClassFileMember {

  override val location
    get() = FieldLocation(
        owner.location,
        name,
        descriptor,
        signature?.takeIf { it.isNotEmpty() },
        modifiers
    )

  override val containingClassFile
    get() = owner

  val name: String
    get() = asmNode.name

  val descriptor: String
    get() = asmNode.desc

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

  val accessType: AccessType
    get() = asmNode.access.getAccessType()

  val modifiers: Modifiers
    get() = Modifiers(asmNode.access)

  val invisibleAnnotations: List<AnnotationNode>
    get() = asmNode.invisibleAnnotations.orEmpty()

}