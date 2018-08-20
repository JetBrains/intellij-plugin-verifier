package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.pluginverifier.misc.singletonOrEmpty
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedClassUsage
import com.jetbrains.pluginverifier.results.experimental.ExperimentalClassUsage
import com.jetbrains.pluginverifier.results.location.ClassLocation
import com.jetbrains.pluginverifier.results.location.FieldLocation
import com.jetbrains.pluginverifier.results.location.Location
import com.jetbrains.pluginverifier.results.location.MethodLocation
import com.jetbrains.pluginverifier.results.modifiers.Modifiers
import com.jetbrains.pluginverifier.results.problems.ClassNotFoundProblem
import com.jetbrains.pluginverifier.results.problems.FailedToReadClassFileProblem
import com.jetbrains.pluginverifier.results.problems.IllegalClassAccessProblem
import com.jetbrains.pluginverifier.results.problems.InvalidClassFileProblem
import com.jetbrains.pluginverifier.results.reference.ClassReference
import com.jetbrains.pluginverifier.verifiers.logic.CommonClassNames
import com.jetbrains.pluginverifier.verifiers.resolution.ClsResolution
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import java.util.*

fun VerificationContext.resolveClassOrProblem(
    className: String,
    lookup: ClassNode,
    lookupLocation: () -> Location
): ClassNode? {
  val resolution = clsResolver.resolveClass(className)
  return with(resolution) {
    when (this) {
      is ClsResolution.Found -> {
        if (!isClassAccessibleToOtherClass(node, lookup)) {
          registerProblem(IllegalClassAccessProblem(node.createClassLocation(), node.access.getAccessType(), lookupLocation()))
          return null
        }
        val classDeprecated = node.getDeprecationInfo()
        if (classDeprecated != null) {
          registerDeprecatedUsage(DeprecatedClassUsage(node.createClassLocation(), lookupLocation(), classDeprecated))
        }
        val experimentalApi = node.isExperimentalApi()
        if (experimentalApi) {
          registerExperimentalApiUsage(ExperimentalClassUsage(node.createClassLocation(), lookupLocation()))
        }
        node
      }
      ClsResolution.ExternalClass -> null
      ClsResolution.NotFound -> {
        registerProblem(ClassNotFoundProblem(ClassReference(className), lookupLocation()))
        null
      }
      is ClsResolution.InvalidClassFile -> {
        registerProblem(InvalidClassFileProblem(ClassReference(className), lookupLocation(), asmError))
        null
      }
      is ClsResolution.FailedToReadClassFile -> {
        registerProblem(FailedToReadClassFileProblem(ClassReference(className), lookupLocation(), reason))
        null
      }
    }
  }
}

fun VerificationContext.checkClassExistsOrExternal(className: String, lookupLocation: () -> Location) {
  if (!clsResolver.isExternalClass(className) && !clsResolver.classExists(className)) {
    registerProblem(ClassNotFoundProblem(ClassReference(className), lookupLocation()))
  }
}

@Suppress("UNCHECKED_CAST")
private fun VerificationContext.resolveAllDirectParents(classNode: ClassNode): List<ClassNode> {
  val parents = classNode.superName.singletonOrEmpty() + (classNode.interfaces as? List<String>).orEmpty()
  return parents.mapNotNull { resolveClassOrProblem(it, classNode) { classNode.createClassLocation() } }
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
  val childClass = (clsResolver.resolveClass(childClassName) as? ClsResolution.Found)?.node ?: return false
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

fun ClassNode.createClassLocation() =
    ClassLocation(
        name,
        signature ?: "",
        Modifiers(access)
    )

fun createMethodLocation(hostClass: ClassNode, method: MethodNode) =
    MethodLocation(
        hostClass.createClassLocation(),
        method.name,
        method.desc,
        method.getParameterNames(),
        method.signature ?: "",
        Modifiers(method.access)
    )

fun createFieldLocation(hostClass: ClassNode, field: FieldNode) =
    FieldLocation(
        hostClass.createClassLocation(),
        field.name,
        field.desc,
        field.signature ?: "",
        Modifiers(field.access)
    )