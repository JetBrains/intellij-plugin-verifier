package com.jetbrains.plugin.structure.impl.resolvers;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.jetbrains.plugin.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Dennis.Ushakov
 */
public final class ContainerResolver extends Resolver {

  private final List<Resolver> myResolvers;
  private final String myPresentableName;

  private ContainerResolver(@NotNull String presentableName, @NotNull List<Resolver> resolvers) {
    myResolvers = new ArrayList<Resolver>(resolvers);
    myPresentableName = presentableName;
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

  @NotNull
  @Override
  public Iterator<String> getAllClasses() {
    List<Iterator<String>> listOfIterators = Lists.transform(myResolvers, new Function<Resolver, Iterator<String>>() {
      @Override
      public Iterator<String> apply(Resolver input) {
        return input.getAllClasses();
      }
    });
    return Iterators.concat(listOfIterators.iterator());
  }

  @Override
  public String toString() {
    return myPresentableName;
  }

  @Override
  public boolean isEmpty() {
    for (Resolver resolver : myResolvers) {
      if (!resolver.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean containsClass(@NotNull final String className) {
    for (Resolver resolver : myResolvers) {
      if (resolver.containsClass(className)) {
        return true;
      }
    }
    return false;
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
    for (Resolver resolver : myResolvers) {
      ClassNode classNode = resolver.findClass(className);
      if (classNode != null) {
        return classNode;
      }
    }
    return null;
  }

  @Override
  @Nullable
  public Resolver getClassLocation(@NotNull String className) {
    for (Resolver resolver : myResolvers) {
      Resolver location = resolver.getClassLocation(className);
      if (location != null) {
        return location;
      }
    }
    return null;
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
