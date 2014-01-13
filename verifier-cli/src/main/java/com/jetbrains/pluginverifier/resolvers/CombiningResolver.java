package com.jetbrains.pluginverifier.resolvers;

import com.jetbrains.pluginverifier.pool.ClassPool;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

public class CombiningResolver implements Resolver {
  private final List<Resolver> myResolvers;

  private CombiningResolver(List<Resolver> resolvers) {
    myResolvers = resolvers;
  }

  @Override
  public ClassNode findClass(final String className) {
    for (Resolver resolver : myResolvers) {
      ClassNode klass = resolver.findClass(className);
      if (klass != null)
        return klass;
    }

    return null;
  }

  public static Resolver union(List<Resolver> resolvers) {
    if (resolvers.isEmpty()) {
      return ClassPool.EMPTY;
    }

    if (resolvers.size() == 1) {
      return resolvers.get(0);
    }

    return new CombiningResolver(resolvers);
  }

  @Override
  public String getClassLocationMoniker(final String className) {
    for (Resolver resolver : myResolvers) {
      String moniker = resolver.getClassLocationMoniker(className);
      if (moniker != null)
        return moniker;
    }

    return null;
  }
}
