package com.jetbrains.pluginverifier.utils

import com.intellij.structure.resolvers.Resolver
import com.jetbrains.pluginverifier.api.VContext

import java.util.*
import java.util.function.Predicate


/**
 * @author Sergey Patrikeev
 */
internal class ParentsVisitor(private val myResolver: Resolver, private val myCtx: VContext) {

  private val myParentsCache = HashMap<String, Set<String>>()

  /**
   * Iterates through the inheritance tree of the specified class and collects
   * all the unresolved parent-classes

   * @param className         start class
   * *
   * @param excludedPredicate whether to treat given class as excluded (and not process it at all)
   * *
   * @return set of unresolved classes
   * *
   * @throws IllegalArgumentException in case starting class is not found in the [resolver][.myResolver]
   */
  @Throws(IllegalArgumentException::class)
  fun collectUnresolvedParents(className: String,
                               excludedPredicate: Predicate<String>): Set<String> {
    val classNode = VerifierUtil.findClass(myResolver, className, myCtx) ?: throw IllegalArgumentException(className + " should be found in the resolver " + myResolver)

    if (myParentsCache.containsKey(className)) {
      return myParentsCache[className]!!
    }

    val allParents = HashSet<String>()

    val superName = classNode.superName
    if (superName != null) {
      if (!excludedPredicate.test(superName)) {
        if (VerifierUtil.findClass(myResolver, superName, myCtx) != null) {
          allParents.addAll(collectUnresolvedParents(superName, excludedPredicate))
        } else {
          allParents.add(superName)
        }
      }
    }

    val interfaces = classNode.interfaces
    if (interfaces != null) {
      @Suppress("UNCHECKED_CAST")
      for (anInterface in interfaces as List<String>) {
        if (!excludedPredicate.test(anInterface)) {
          if (VerifierUtil.findClass(myResolver, anInterface, myCtx) != null) {
            allParents.addAll(collectUnresolvedParents(anInterface, excludedPredicate))
          } else {
            allParents.add(anInterface)
          }
        }
      }
    }

    myParentsCache.put(className, allParents)
    return allParents
  }

}
