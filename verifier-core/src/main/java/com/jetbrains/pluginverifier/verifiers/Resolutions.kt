package com.jetbrains.pluginverifier.verifiers

import com.jetbrains.plugin.structure.classes.resolvers.InvalidClassFileException
import com.jetbrains.pluginverifier.misc.singletonOrEmpty
import com.jetbrains.pluginverifier.results.deprecated.DeprecatedClassUsage
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
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

sealed class ClsResolution {
  object NotFound : ClsResolution()
  object ExternalClass : ClsResolution()
  data class InvalidClassFile(val asmError: String) : ClsResolution()
  data class FailedToReadClassFile(val reason: String) : ClsResolution()
  data class Found(val node: ClassNode) : ClsResolution()
}

private val ClassFileLogger: Logger = LoggerFactory.getLogger("plugin.verifier.class.file.read.logger")

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
    verificationClassLoader.findClass(className)
  } catch (e: InvalidClassFileException) {
    ClassFileLogger.debug("Unable to read invalid class $className", e)
    return ClsResolution.InvalidClassFile(e.asmError)
  } catch (e: Exception) {
    ClassFileLogger.info("Unable to read class $className", e)
    return ClsResolution.FailedToReadClassFile(e.message ?: e.javaClass.name)
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
          registerProblem(IllegalClassAccessProblem(node.createClassLocation(), node.access.getAccessType(), lookupLocation()))
          return null
        }
        if (node.isDeprecated()) {
          registerDeprecatedUsage(DeprecatedClassUsage(node.createClassLocation(), lookupLocation()))
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

private fun VerificationContext.classExists(className: String) = verificationClassLoader.containsClass(className)

//todo: check the cases when the accessibility must be checked.
fun VerificationContext.checkClassExistsOrExternal(className: String, lookupLocation: () -> Location) {
  if (!isExternalClass(className) && !classExists(className)) {
    registerProblem(ClassNotFoundProblem(ClassReference(className), lookupLocation()))
  }
}

@Suppress("UNCHECKED_CAST")
private fun VerificationContext.resolveAllDirectParents(classNode: ClassNode): List<ClassNode> {
  val parents = classNode.superName.singletonOrEmpty() + (classNode.interfaces as? List<String>).orEmpty()
  return parents.mapNotNull { resolveClassOrProblem(it, classNode, { classNode.createClassLocation() }) }
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