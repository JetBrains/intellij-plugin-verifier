package com.intellij.structure.impl.resolvers;

import com.intellij.structure.bytecode.ClassFile;
import com.intellij.structure.pool.EmptyClassPool;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
  public ClassFile findClass(@NotNull final String className) {
    for (Resolver resolver : myResolvers) {
      ClassFile klass = resolver.findClass(className);
      if (klass != null)
        return klass;
    }

    return null;
  }

  @Override
  public Resolver getClassLocation(@NotNull final String className) {
    for (Resolver resolver : myResolvers) {
      Resolver inner = resolver.getClassLocation(className);
      if (inner != null)
        return inner;
    }

    return null;
  }

  @Nullable
  @Override
  public String getMoniker() {
    return null;
  }
}
