package com.intellij.structure.impl.resolvers;

import com.intellij.structure.pool.EmptyClassPool;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

public class CombiningResolver implements Resolver {
  private final List<Resolver> myResolvers;

  private CombiningResolver(List<Resolver> resolvers) {
    myResolvers = resolvers;
  }

  public static Resolver union(List<Resolver> resolvers) {
    if (resolvers.isEmpty()) {
      return EmptyClassPool.INSTANCE;
    }

    if (resolvers.size() == 1) {
      return resolvers.get(0);
    }

    return new CombiningResolver(resolvers);
  }

  @Override
  public ClassNode findClass(@NotNull final String className) {
    for (Resolver resolver : myResolvers) {
      ClassNode klass = resolver.findClass(className);
      if (klass != null)
        return klass;
    }

    return null;
  }

  @Override
  public String getClassLocationMoniker(@NotNull final String className) {
    for (Resolver resolver : myResolvers) {
      String moniker = resolver.getClassLocationMoniker(className);
      if (moniker != null)
        return moniker;
    }

    return null;
  }
}
