package com.jetbrains.pluginverifier.pool;

import org.objectweb.asm.tree.ClassNode;

import java.util.*;

/**
 * @author Dennis.Ushakov
 */
public class ContainerClassPool implements ClassPool {
  private final List<ClassPool> myPools = new ArrayList<ClassPool>();
  private final String myName;

  public ContainerClassPool(final String name, final List<ClassPool> pools) {
    myName = name;
    myPools.addAll(pools);
  }

  public ClassNode getClassNode(String className) {
    for (ClassPool pool : myPools) {
      final ClassNode node = pool.getClassNode(className);
      if (node != null) return node;
    }
    return null;
  }

  @Override
  public String getClassLocationMoniker(final String className) {
    for (ClassPool pool : myPools) {
      final String moniker = pool.getClassLocationMoniker(className);
      if (moniker != null) return moniker;
    }
    return null;
  }

  public Collection<String> getAllClasses() {
    final Set<String> result = new HashSet<String>();
    for (ClassPool pool : myPools) {
      result.addAll(pool.getAllClasses());
    }
    return result;
  }

  public String getMoniker() {
    return myName;
  }

  @Override
  public boolean isEmpty() {
    for (ClassPool pool : myPools) {
      if (!pool.isEmpty())
        return false;
    }

    return true;
  }
}
