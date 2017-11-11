package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.misc.singletonOrEmpty
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedClassUsage
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.IllegalClassAccessProblem
import com.jetbrains.pluginverifier.results.problems.InvalidClassFileProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.verifiers.logic.CommonClassNames
import org.objectweb.asm.tree.ClassNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

sealed class ClsResolution {
  object NotFound : ClsResolution()
  object ExternalClass : ClsResolution()
  data class InvalidClassFile(val reason: String) : ClsResolution()
  data class Found(val node: ClassNode) : ClsResolution()
}

private val ClassFileLogger: Logger = LoggerFactory.getLogger("plugin.verifier.class.file.reader")

/**
 * To resolve an unresolved symbolic reference from D to a class or interface C denoted by N, the following steps are performed:
 * ...<JVM-related stuff>...
 *  3) Finally, access permissions to C are checked.
 *  If C is not accessible (§5.4.4) to D, class or interface resolution throws an IllegalAccessError.
 */
private fun VerificationContext.resolveClass(className: String): ClsResolution {
  if (isExternalClass(className)) {
    return ClsResolution.ExternalClass
  }
  val node = try {
    classLoader.findClass(className)
  } catch (e: Exception) {
    ClassFileLogger.debug("Unable to read class $className", e)
    return ClsResolution.InvalidClassFile("Unable to read class-file $className using ASM Java Bytecode engineering library. Internal error: ${e.message}")
  }
  return node?.let { ClsResolution.Found(it) } ?: ClsResolution.NotFound
}

fun VerificationContext.resolveClassOrProblem(className: String,
                                              lookup: ClassNode,
                                              lookupLocation: () -> Location): ClassNode? {
  val resolution = resolveClass(className)
  return with(resolution) {
    when (this) {
      is ClsResolution.Found -> {
        if (!isClassAccessibleToOtherClass(node, lookup)) {
          registerProblem(IllegalClassAccessProblem(fromClass(node), node.access.getAccessType(), lookupLocation()))
          return null
        }
        if (node.isDeprecated()) {
          registerDeprecatedUsage(DeprecatedClassUsage(fromClass(node), lookupLocation()))
        }
        node
      }
      ClsResolution.ExternalClass -> null
      ClsResolution.NotFound -> {
        registerProblem(ClassNotFoundProblem(ClassReference(className), lookupLocation()))
        null
      }
      is ClsResolution.InvalidClassFile -> {
        registerProblem(InvalidClassFileProblem(ClassReference(className), lookupLocation(), reason))
        null
      }
    }
  }
}

//todo: check the cases when the accessibility must be checked.
fun VerificationContext.checkClassExistsOrExternal(className: String, lookupLocation: () -> Location) {
  val resolution = resolveClass(className)
  return when (resolution) {
    ClsResolution.NotFound -> {
      registerProblem(ClassNotFoundProblem(ClassReference(className), lookupLocation()))
    }
    is ClsResolution.InvalidClassFile -> {
      registerProblem(InvalidClassFileProblem(ClassReference(className), lookupLocation(), resolution.reason))
    }
    ClsResolution.ExternalClass -> Unit
    is ClsResolution.Found -> Unit
  }
}

@Suppress("UNCHECKED_CAST")
private fun VerificationContext.resolveAllDirectParents(classNode: ClassNode): List<ClassNode> {
  val parents = classNode.superName.singletonOrEmpty() + (classNode.interfaces as? List<String>).orEmpty()
  return parents.mapNotNull { resolveClassOrProblem(it, classNode, { fromClass(classNode) }) }
}

fun VerificationContext.isSubclassOf(child: ClassNode, possibleParent: ClassNode): Boolean =
    isSubclassOf(child, possibleParent.name)

fun VerificationContext.isSubclassOrSelf(childClassName: String, possibleParentName: String): Boolean {
  if (childClassName == possibleParentName) {
    return true
  }
  return isSubclassOf(childClassName, possibleParentName)
}

fun VerificationContext.isSubclassOf(childClassName: String, possibleParentName: String): Boolean {
  val childClass = (resolveClass(childClassName) as? ClsResolution.Found)?.node ?: return false
  return isSubclassOf(childClass, possibleParentName)
}

fun VerificationContext.isSubclassOf(child: ClassNode, possibleParentName: String): Boolean {
  if (possibleParentName == CommonClassNames.JAVA_LANG_OBJECT) {
    return true
  }

  val directParents = resolveAllDirectParents(child)

  val queue = LinkedList<ClassNode>()
  queue.addAll(directParents)

  val visited = hashSetOf<String>()
  visited.addAll(directParents.map { it.name })

  while (queue.isNotEmpty()) {
    val node = queue.poll()
    if (node.name == possibleParentName) {
      return true
    }

    resolveAllDirectParents(node).filterNot { it.name in visited }.forEach {
      visited.add(it.name)
      queue.addLast(it)
    }
  }

  return false
}

