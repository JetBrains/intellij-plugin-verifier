package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

fun ClassNode.createClassLocation() =
    ClassLocation(
        name,
        signature ?: "",
        Modifiers(access)
    )

fun createMethodLocation(
    hostClass: ClassNode,
    method: MethodNode
) = MethodLocation(
    hostClass.createClassLocation(),
    method.name,
    method.desc,
    method.getParameterNames(),
    method.signature ?: "",
    Modifiers(method.access)
)

fun createFieldLocation(
    hostClass: ClassNode,
    field: FieldNode
) = FieldLocation(
    hostClass.createClassLocation(),
    field.name,
    field.desc,
    field.signature ?: "",
    Modifiers(field.access)
)

/**
 * Returns internal package name of this [Location],
 * where all '.' are replaced with '/'.
 * Returns empty string "" for the default package.
 */
fun Location.getPackageInternalName(): String =
    when (this) {
      is ClassLocation -> className
      is FieldLocation -> hostClass.className
      is MethodLocation -> hostClass.className
    }.substringBeforeLast('/', "")
