package com.intellij.structure.impl.resolvers;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.*;

/**
 * @author Dennis.Ushakov
 */
public class ContainerResolver extends Resolver {

  private final Map<String, Resolver> myClassToResolver = new HashMap<String, Resolver>();
  private final String myPresentableName;

  private ContainerResolver(@NotNull String presentableName, @NotNull List<Resolver> resolvers) {
    myPresentableName = presentableName;
    fillClassMap(resolvers);
  }

  @NotNull
  public static Resolver createFromList(@NotNull String presentableName, @NotNull List<Resolver> resolvers) {
    Resolver nonEmpty = null;
    for (Resolver pool : resolvers) {
      if (!pool.isEmpty()) {
        if (nonEmpty == null) {
          nonEmpty = pool;
        } else {
          return new ContainerResolver(presentableName, resolvers);
        }
      }
    }
    if (nonEmpty == null) {
      return EmptyResolver.INSTANCE;
    }
    if (resolvers.size() == 1) {
      return nonEmpty;
    }
    return nonEmpty;
  }

  private void fillClassMap(@NotNull List<Resolver> resolvers) {
    //the class will be mapped to the first containing resolver
    for (int i = resolvers.size() - 1; i >= 0; i--) {
      Resolver resolver = resolvers.get(i);
      for (String aClass : resolver.getAllClasses()) {
        myClassToResolver.put(aClass, resolver);
      }
    }
  }

  @NotNull
  @Override
  public Set<String> getAllClasses() {
    return Collections.unmodifiableSet(myClassToResolver.keySet());
  }

  @Override
  public String toString() {
    return myPresentableName;
  }

  @Override
  public boolean isEmpty() {
    return myClassToResolver.isEmpty();
  }

  @Override
  public boolean containsClass(@NotNull String className) {
    return myClassToResolver.containsKey(className);
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) throws IOException {
    if (!myClassToResolver.containsKey(className)) {
      return null;
    }
    return myClassToResolver.get(className).findClass(className);
  }

  @Override
  @Nullable
  public Resolver getClassLocation(@NotNull String className) {
    if (!myClassToResolver.containsKey(className)) {
      return null;
    }
    return myClassToResolver.get(className).getClassLocation(className);
  }

  @Override
  public void close() {
    for (Resolver resolver : myClassToResolver.values()) {
      resolver.close();
    }
  }
}
