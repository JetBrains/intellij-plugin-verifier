package com.jetbrains.pluginverifier.location

import org.jetbrains.annotations.TestOnly
import org.objectweb.asm.tree.MethodNode

/**
 * @author Sergey Patrikeev
 */
abstract class ProblemLocation {

  abstract fun asString(): String

  final override fun toString(): String = asString()

  final override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || other.javaClass != javaClass) return false
    return asString() == (other as ProblemLocation).asString()
  }

  final override fun hashCode(): Int = asString().hashCode()

  companion object {

    //TODO: add more detailed location, e.g. superclass, field of a class, interface, throws list and so on

    fun fromPlugin(pluginId: String): ProblemLocation = PluginLocation(pluginId)

    fun fromClass(className: String): ProblemLocation = CodeLocation(className, null, null)

    fun fromField(className: String, fieldName: String): ProblemLocation = CodeLocation(className, null, fieldName)

    @TestOnly
    fun fromMethod(className: String, methodDescr: String): ProblemLocation = CodeLocation(className, methodDescr, null)

    fun fromMethod(className: String, methodNode: MethodNode): ProblemLocation = CodeLocation(className, getMethodDescr(methodNode), null)

    private fun getMethodDescr(methodNode: MethodNode): String = methodNode.name + methodNode.desc
  }
}
