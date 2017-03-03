package com.intellij.structure.impl.resolvers;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Dennis.Ushakov
 */
public class ContainerResolver extends Resolver {

  private final List<Resolver> myResolvers;
  private final Map<String, Resolver> myClassToResolver = new HashMap<String, Resolver>();
  private final String myPresentableName;

  private ContainerResolver(@NotNull String presentableName, @NotNull List<Resolver> resolvers) {
    myResolvers = new ArrayList<Resolver>(resolvers);
    myPresentableName = presentableName;
    fillClassMap();
  }

  @NotNull
  public static Resolver createFromList(@NotNull String presentableName, @NotNull List<Resolver> resolvers) {
    List<Resolver> nonEmptyResolvers = new ArrayList<Resolver>();
    for (Resolver resolver : resolvers) {
      if (!resolver.isEmpty()) {
        nonEmptyResolvers.add(resolver);
      }
    }
    if (nonEmptyResolvers.isEmpty()) {
      return EmptyResolver.INSTANCE;
    }
    if (nonEmptyResolvers.size() == 1) {
      return nonEmptyResolvers.get(0);
    }
    return new ContainerResolver(presentableName, nonEmptyResolvers);
  }

  private void fillClassMap() {
    //the class will be mapped to the first containing resolver
    for (int i = myResolvers.size() - 1; i >= 0; i--) {
      Resolver resolver = myResolvers.get(i);
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

  @NotNull
  @Override
  public List<File> getClassPath() {
    List<File> result = new ArrayList<File>();
    for (Resolver resolver : myResolvers) {
      result.addAll(resolver.getClassPath());
    }
    return result;
  }

  @Override
  @Nullable
  public ClassNode findClass(@NotNull String className) throws IOException {
    Resolver resolver = myClassToResolver.get(className);
    return resolver == null ? null : resolver.findClass(className);
  }

  @Override
  @Nullable
  public Resolver getClassLocation(@NotNull String className) {
    Resolver resolver = myClassToResolver.get(className);
    return resolver == null ? null : resolver.getClassLocation(className);
  }

  @Override
  public void close() throws IOException {
    IOException first = null;
    for (Resolver resolver : myResolvers) {
      try {
        resolver.close();
      } catch (IOException e) {
        first = e;
      }
    }
    if (first != null) {
      throw first;
    }
  }
}
