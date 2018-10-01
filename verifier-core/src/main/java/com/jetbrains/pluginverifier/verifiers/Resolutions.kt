package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.misc.singletonOrEmpty
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedClassUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalClassUsage
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.FailedToReadClassFileProblem
import com.jetbrains.pluginverifier.results.problems.IllegalClassAccessProblem
import com.jetbrains.pluginverifier.results.problems.InvalidClassFileProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.verifiers.logic.CommonClassNames
import com.jetbrains.pluginverifier.verifiers.resolution.ClsResolution
import com.jetbrains.pluginverifier.verifiers.resolution.ClsResolver
import org.objectweb.asm.tree.ClassNode
import java.util.*

fun ClsResolver.resolveClassOrProblem(
    className: String,
    lookup: ClassNode,
    problemRegistrar: ProblemRegistrar,
    lookupLocation: () -> Location
): ClassNode? {
  val resolution = resolveClass(className)
  return with(resolution) {
    when (this) {
      is ClsResolution.Found -> {
        if (!isClassAccessibleToOtherClass(node, lookup)) {
          problemRegistrar.registerProblem(IllegalClassAccessProblem(node.createClassLocation(), node.access.getAccessType(), lookupLocation()))
          return null
        }
        val classDeprecated = node.getDeprecationInfo()
        if (classDeprecated != null) {
          problemRegistrar.registerDeprecatedUsage(DeprecatedClassUsage(node.createClassLocation(), lookupLocation(), classDeprecated))
        }
        val experimentalApi = node.isExperimentalApi()
        if (experimentalApi) {
          problemRegistrar.registerExperimentalApiUsage(ExperimentalClassUsage(node.createClassLocation(), lookupLocation()))
        }
        node
      }
      ClsResolution.ExternalClass -> null
      ClsResolution.NotFound -> {
        problemRegistrar.registerProblem(ClassNotFoundProblem(ClassReference(className), lookupLocation()))
        null
      }
      is ClsResolution.InvalidClassFile -> {
        problemRegistrar.registerProblem(InvalidClassFileProblem(ClassReference(className), lookupLocation(), asmError))
        null
      }
      is ClsResolution.FailedToReadClassFile -> {
        problemRegistrar.registerProblem(FailedToReadClassFileProblem(ClassReference(className), lookupLocation(), reason))
        null
      }
    }
  }
}


fun VerificationContext.resolveClassOrProblem(
    className: String,
    lookup: ClassNode,
    lookupLocation: () -> Location
): ClassNode? {
  return clsResolver.resolveClassOrProblem(className, lookup, this, lookupLocation)
}

fun VerificationContext.checkClassExistsOrExternal(className: String, lookupLocation: () -> Location) {
  if (!clsResolver.isExternalClass(className) && !clsResolver.classExists(className)) {
    registerProblem(ClassNotFoundProblem(ClassReference(className), lookupLocation()))
  }
}

@Suppress("UNCHECKED_CAST")
private fun ClsResolver.resolveAllDirectParents(classNode: ClassNode, problemRegistrar: ProblemRegistrar): List<ClassNode> {
  val parents = classNode.superName.singletonOrEmpty() + classNode.getInterfaces().orEmpty()
  return parents.mapNotNull { resolveClassOrProblem(it, classNode, problemRegistrar) { classNode.createClassLocation() } }
}

fun ClsResolver.isSubclassOf(child: ClassNode, possibleParent: ClassNode, problemRegistrar: ProblemRegistrar): Boolean =
    isSubclassOf(child, possibleParent.name, problemRegistrar)

fun VerificationContext.isSubclassOf(child: ClassNode, possibleParent: ClassNode): Boolean =
    clsResolver.isSubclassOf(child, possibleParent, this)

fun VerificationContext.isSubclassOrSelf(childClassName: String, possibleParentName: String): Boolean {
  if (childClassName == possibleParentName) {
    return true
  }
  return isSubclassOf(childClassName, possibleParentName)
}

fun VerificationContext.isSubclassOf(childClassName: String, possibleParentName: String): Boolean {
  val childClass = (clsResolver.resolveClass(childClassName) as? ClsResolution.Found)?.node ?: return false
  return clsResolver.isSubclassOf(childClass, possibleParentName, this)
}

fun ClsResolver.isSubclassOf(
    child: ClassNode,
    possibleParentName: String,
    problemRegistrar: ProblemRegistrar
): Boolean {
  if (possibleParentName == CommonClassNames.JAVA_LANG_OBJECT) {
    return true
  }

  val directParents = resolveAllDirectParents(child, problemRegistrar)

  val queue = LinkedList<ClassNode>()
  queue.addAll(directParents)

  val visited = hashSetOf<String>()
  visited.addAll(directParents.map { it.name })

  while (queue.isNotEmpty()) {
    val node = queue.poll()
    if (node.name == possibleParentName) {
      return true
    }

    resolveAllDirectParents(node, problemRegistrar).filterNot { it.name in visited }.forEach {
      visited.add(it.name)
      queue.addLast(it)
    }
  }

  return false
}