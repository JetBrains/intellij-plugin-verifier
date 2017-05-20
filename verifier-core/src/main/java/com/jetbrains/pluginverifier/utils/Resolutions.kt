package com.jetbrains.pluginverifier.utils

import com.jetbrains.pluginverifier.location.Location
import com.jetbrains.pluginverifier.problems.AccessType
import com.jetbrains.pluginverifier.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.problems.IllegalClassAccessProblem
import com.jetbrains.pluginverifier.problems.InvalidClassFileProblem
import com.jetbrains.pluginverifier.reference.ClassReference
import com.jetbrains.pluginverifier.verifiers.VerificationContext
import org.jetbrains.intellij.plugins.internal.asm.tree.ClassNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

sealed class ClsResolution {
  object NotFound : ClsResolution()
  object ExternalClass : ClsResolution()
  data class InvalidClassFile(val reason: String) : ClsResolution()
  data class IllegalAccess(val resolvedNode: ClassNode, val accessType: AccessType) : ClsResolution()
  data class Found(val node: ClassNode) : ClsResolution()
}

private val ClassFileLogger: Logger = LoggerFactory.getLogger("plugin.verifier.class.file.reader")

/**
 * To resolve an unresolved symbolic reference from D to a class or interface C denoted by N, the following steps are performed:
 * ...<JVM-related stuff>...
 *  3) Finally, access permissions to C are checked.
 *  If C is not accessible (§5.4.4) to D, class or interface resolution throws an IllegalAccessError.
 */
fun VerificationContext.resolveClass(className: String, lookup: ClassNode): ClsResolution {
  if (verifierParams.isExternalClass(className)) {
    return ClsResolution.ExternalClass
  }
  val node = try {
    resolver.findClass(className)
  } catch (e: Exception) {
    ClassFileLogger.debug("Unable to read class $className", e)
    return ClsResolution.InvalidClassFile("Unable to read class-file $className using ASM Java Bytecode engineering library. Internal error: ${e.message}")
  }
  if (node != null) {
    return if (BytecodeUtil.isClassAccessibleToOtherClass(node, lookup)) {
      ClsResolution.Found(node)
    } else {
      ClsResolution.IllegalAccess(node, BytecodeUtil.getAccessType(node.access))
    }
  }
  return ClsResolution.NotFound
}


fun VerificationContext.resolveClassOrProblem(className: String,
                                              lookup: ClassNode,
                                              lookupLocation: () -> Location): ClassNode? {
  val resolution = resolveClass(className, lookup)
  return when (resolution) {
    is ClsResolution.Found -> resolution.node
    ClsResolution.ExternalClass -> null
    ClsResolution.NotFound -> {
      registerProblem(ClassNotFoundProblem(ClassReference(className), lookupLocation()))
      null
    }
    is ClsResolution.IllegalAccess -> {
      registerProblem(IllegalClassAccessProblem(fromClass(resolution.resolvedNode), resolution.accessType, lookupLocation()))
      null
    }
    is ClsResolution.InvalidClassFile -> {
      registerProblem(InvalidClassFileProblem(ClassReference(className), lookupLocation(), resolution.reason))
      null
    }
  }
}

fun VerificationContext.checkClassExistsOrExternal(className: String, registerMissing: () -> Location) {
  if (!verifierParams.isExternalClass(className) && !resolver.containsClass(className)) {
    registerProblem(ClassNotFoundProblem(ClassReference(className), registerMissing.invoke()))
  }
}

fun VerificationContext.isSubclassOf(child: ClassNode, possibleParent: ClassNode): Boolean {
  var current: ClassNode? = child
  while (current != null) {
    if (possibleParent.name == current.name) {
      return true
    }
    val superName = current.superName ?: return false
    current = resolveClassOrProblem(superName, current, { fromClass(current!!) })
  }
  return false
}

