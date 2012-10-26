package com.jetbrains.pluginverifier.resolvers;

import org.objectweb.asm.tree.ClassNode;

import java.util.List;

public class CombiningResolver implements Resolver {
  private final List<Resolver> myResolvers;

  public CombiningResolver(List<Resolver> resolvers) {
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
