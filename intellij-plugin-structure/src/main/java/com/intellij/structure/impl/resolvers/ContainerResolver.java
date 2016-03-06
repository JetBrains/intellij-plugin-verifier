package com.intellij.structure.impl.resolvers;

import com.google.common.base.Joiner;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;

/**
 * @author Dennis.Ushakov
 */
public class ContainerResolver extends Resolver {

  private final List<Resolver> myResolvers = new ArrayList<Resolver>();

  public ContainerResolver(@NotNull List<Resolver> resolvers) {
    myResolvers.addAll(resolvers);
  }

  @NotNull
  @Override
  public Collection<String> getAllClasses() {
    Set<String> result = new HashSet<String>();
    for (Resolver pool : myResolvers) {
      result.addAll(pool.getAllClasses());
    }
    return result;
  }

  @Override
  public String toString() {
    return Joiner.on(", ").join(myResolvers);
  }

  @Override
  public boolean isEmpty() {
    for (Resolver pool : myResolvers) {
      if (!pool.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) {
    for (Resolver pool : myResolvers) {
      ClassNode node = pool.findClass(className);
      if (node != null) {
        return node;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public Resolver getClassLocation(@NotNull String className) {
    for (Resolver pool : myResolvers) {
      Resolver inner = pool.getClassLocation(className);
      if (inner != null) {
        return inner;
      }
    }
    return null;
  }

}
