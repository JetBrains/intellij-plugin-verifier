package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
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