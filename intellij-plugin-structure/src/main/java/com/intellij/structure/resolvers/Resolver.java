package com.intellij.structure.resolvers;


import com.intellij.structure.impl.resolvers.CacheResolver;
import com.intellij.structure.impl.resolvers.ContainerResolver;
import com.intellij.structure.impl.resolvers.EmptyResolver;
import com.intellij.structure.impl.resolvers.JarFileResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Provides access to byte-code of class by its name
 */
public abstract class Resolver {

  /**
   * Returns resolver which combines given list of resolvers
   *
   *
   * @param presentableName presentableName
   * @param resolvers list of resolvers
   * @return combining resolver
   */
  @NotNull
  public static Resolver createUnionResolver(@NotNull String presentableName, @NotNull List<Resolver> resolvers) {
    return ContainerResolver.createFromList(presentableName, resolvers);
  }

  @NotNull
  public static Resolver createCacheResolver(@NotNull Resolver delegate) {
    return new CacheResolver(delegate);
  }

  /**
   * Creates a resolver from the given File
   *
   * @param jarFile file - should be a .jar archive
   * @return Resolver for the given .jar file
   * @throws IOException              if io-error occurs
   * @throws IllegalArgumentException if supplied file doesn't exist or is not a .jar archive
   */
  @NotNull
  public static Resolver createJarResolver(@NotNull File jarFile) throws IOException {
    return new JarFileResolver(jarFile);
  }

  @NotNull
  public static Resolver getEmptyResolver() {
    return EmptyResolver.INSTANCE;
  }

  /**
   * Returns a class-file node
   *
   * @param className class name in <i>binary</i> form (see JVM specification)
   * @return bytecode accessor
   * @throws IOException if IO error occurs, e.g. file .class-file was deleted
   */
  @Nullable
  public abstract ClassNode findClass(@NotNull String className) throws IOException;

  /**
   * Returns actual class holder which contains a specified class.
   * <p>
   * e.g.: if {@code this} is a containing-resolver (it has inner resolvers), then the method will return the innermost
   * resolver which really contains a class
   * <p>
   * it is meant to use in the following use-case: given some uber-jar we may request the very .jar-file resolver from
   * which this class occurred
   *
   * @param className class name for which resolver should be found (in <i>binary</i> form)
   * @return actual class resolver or {@code null} if {@code this} resolver doesn't contain a specified class
   */
  @Nullable
  public abstract Resolver getClassLocation(@NotNull String className);

  /**
   * Returns <i>binary</i> names of all containing classes
   *
   * @return all classes
   */
  @NotNull
  public abstract Set<String> getAllClasses();


  /**
   * Checks whether this resolver contains any class. Classes can be obtained through {@link #getAllClasses()}
   *
   * @return true if this resolver is not empty, false otherwise
   */
  public abstract boolean isEmpty();
}
