package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.results.deprecated.DeprecatedClassUsage
import com.jetbrains.pluginverifier.results.deprecated.DiscouragingJdkClassUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalClassUsage
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.FailedToReadClassFileProblem
import com.jetbrains.pluginverifier.results.problems.IllegalClassAccessProblem
import com.jetbrains.pluginverifier.results.problems.InvalidClassFileProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.verifiers.logic.CommonClassNames
import com.jetbrains.pluginverifier.verifiers.resolution.ClassFileOrigin
import com.jetbrains.pluginverifier.verifiers.resolution.ClassResolution
import org.objectweb.asm.tree.ClassNode
import java.util.*

fun VerificationContext.resolveClassOrProblem(
    className: String,
    lookup: ClassNode,
    lookupLocation: () -> Location
): ClassNode? {
  val resolution = classResolver.resolveClass(className)
  return when (resolution) {
    is ClassResolution.Found -> {
      val node = resolution.node
      if (!isClassAccessibleToOtherClass(node, lookup)) {
        registerProblem(IllegalClassAccessProblem(node.createClassLocation(), node.access.getAccessType(), lookupLocation()))
        return null
      }
      val classDeprecated = node.getDeprecationInfo()
      if (classDeprecated != null) {
        registerDeprecatedUsage(DeprecatedClassUsage(node.createClassLocation(), lookupLocation(), classDeprecated))
      }
      if (node.isDiscouragingJdkClass()) {
        val classOrigin = classResolver.getOriginOfClass(node.name)
        if (classOrigin is ClassFileOrigin.IdeClass || classOrigin is ClassFileOrigin.JdkClass) {
          val isClassProvidedByIde = classOrigin is ClassFileOrigin.IdeClass
          registerDeprecatedUsage(DiscouragingJdkClassUsage(node.createClassLocation(), lookupLocation(), isClassProvidedByIde))
        }
      }
      val experimentalApi = node.isExperimentalApi()
      if (experimentalApi) {
        registerExperimentalApiUsage(ExperimentalClassUsage(node.createClassLocation(), lookupLocation()))
      }
      node
    }
    ClassResolution.ExternalClass -> null
    ClassResolution.NotFound -> {
      registerProblem(ClassNotFoundProblem(ClassReference(className), lookupLocation()))
      null
    }
    is ClassResolution.InvalidClassFile -> {
      registerProblem(InvalidClassFileProblem(ClassReference(className), lookupLocation(), resolution.asmError))
      null
    }
    is ClassResolution.FailedToReadClassFile -> {
      registerProblem(FailedToReadClassFileProblem(ClassReference(className), lookupLocation(), resolution.reason))
      null
    }
  }
}

private fun VerificationContext.resolveAllDirectParents(classNode: ClassNode): List<ClassNode> {
  val parents = listOfNotNull(classNode.superName) + classNode.getInterfaces().orEmpty()
  return parents.mapNotNull { resolveClassOrProblem(it, classNode) { classNode.createClassLocation() } }
}

fun VerificationContext.isSubclassOrSelf(childClassName: String, possibleParentName: String): Boolean {
  if (childClassName == possibleParentName) {
    return true
  }
  return isSubclassOf(childClassName, possibleParentName)
}

fun VerificationContext.isSubclassOf(childClassName: String, possibleParentName: String): Boolean {
  val childClass = (classResolver.resolveClass(childClassName) as? ClassResolution.Found)?.node ?: return false
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