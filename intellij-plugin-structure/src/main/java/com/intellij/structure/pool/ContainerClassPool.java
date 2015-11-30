package com.intellij.structure.pool;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;

/**
 * @author Dennis.Ushakov
 */
public class ContainerClassPool implements ClassPool {

  private final List<ClassPool> myClassPools = new ArrayList<ClassPool>();

  private final String myMoniker;

  private ContainerClassPool(@NotNull String moniker,
                             @NotNull List<ClassPool> classPools) {
    myMoniker = moniker;
    myClassPools.addAll(classPools);
  }

  public static ClassPool getUnion(@NotNull String moniker,
                                   @NotNull List<ClassPool> classPools) {
    ClassPool someNonEmptyPool = null;
    for (ClassPool pool : classPools) {
      if (!pool.isEmpty()) {
        if (someNonEmptyPool == null) {
          someNonEmptyPool = pool;
        } else {
          return new ContainerClassPool(moniker, classPools);
        }
      }
    }
    if (someNonEmptyPool == null) {
      return ClassPool.EMPTY_POOL;
    }
    return someNonEmptyPool;
  }

  @NotNull
  @Override
  public Collection<String> getAllClasses() {
    Set<String> result = new HashSet<String>();
    for (ClassPool pool : myClassPools) {
      result.addAll(pool.getAllClasses());
    }
    return result;
  }

  @NotNull
  @Override
  public String getMoniker() {
    return myMoniker;
  }

  @Override
  public boolean isEmpty() {
    for (ClassPool pool : myClassPools) {
      if (!pool.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) {
    for (ClassPool pool : myClassPools) {
      ClassNode node = pool.findClass(className);
      if (node != null) {
        return node;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public String getClassLocationMoniker(@NotNull String className) {
    for (ClassPool pool : myClassPools) {
      String moniker = pool.getClassLocationMoniker(className);
      if (moniker != null) {
        return moniker;
      }
    }
    return null;
  }

}
