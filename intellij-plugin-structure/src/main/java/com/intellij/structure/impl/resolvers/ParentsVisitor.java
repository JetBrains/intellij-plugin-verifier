package com.intellij.structure.impl.resolvers;

import com.google.common.base.Predicate;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;


/**
 * @author Sergey Patrikeev
 */
public class ParentsVisitor {

  private final Map<String, Set<String>> myParentsCache = new HashMap<String, Set<String>>();

  private final Resolver myResolver;

  public ParentsVisitor(@NotNull Resolver resolver) {
    myResolver = resolver;
  }

  /**
   * Iterates through the inheritance tree of the specified class and collects
   * all the unresolved parent-classes
   *
   * @param className         start class
   * @param excludedPredicate whether to treat given class as excluded (and not process it at all)
   * @return set of unresolved classes
   * @throws IllegalArgumentException in case starting class is not found in the {@link #myResolver resolver}
   */
  @NotNull
  public Set<String> collectUnresolvedParents(@NotNull String className,
                                              @NotNull Predicate<String> excludedPredicate) throws IllegalArgumentException {
    ClassNode classNode = myResolver.findClass(className);
    if (classNode == null) {
      throw new IllegalArgumentException(className + " should be found in the resolver " + myResolver);
    }

    if (myParentsCache.containsKey(className)) {
      return myParentsCache.get(className);
    }

    Set<String> allParents = new HashSet<String>();

    String superName = classNode.superName;
    if (superName != null) {
      if (!excludedPredicate.apply(superName)) {
        if (myResolver.findClass(superName) != null) {
          allParents.addAll(collectUnresolvedParents(superName, excludedPredicate));
        } else {
          allParents.add(superName);
        }
      }
    }

    @SuppressWarnings("unchecked")
    List<String> interfaces = classNode.interfaces;
    if (interfaces != null) {
      for (String anInterface : interfaces) {
        if (!excludedPredicate.apply(anInterface)) {
          if (myResolver.findClass(anInterface) != null) {
            allParents.addAll(collectUnresolvedParents(anInterface, excludedPredicate));
          } else {
            allParents.add(anInterface);
          }
        }
      }
    }

    myParentsCache.put(className, allParents);
    return allParents;
  }

}
