package com.intellij.structure.resolvers;


import com.intellij.structure.impl.resolvers.CacheResolver;
import com.intellij.structure.impl.resolvers.ContainerResolver;
import com.intellij.structure.impl.resolvers.EmptyResolver;
import com.intellij.structure.impl.resolvers.SoftJarResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipFile;

/**
 * Provides access to byte-code of class by its name
 */
public abstract class Resolver {

  /**
   * Returns resolver which combines given list of resolvers
   *
   * @param resolvers list of resolvers
   * @return combining resolver
   */
  @NotNull
  public static Resolver createUnionResolver(@NotNull List<Resolver> resolvers) {
    return ContainerResolver.createFromList(resolvers);
  }

  @NotNull
  public static Resolver createCacheResolver(@NotNull Resolver delegate) {
    return new CacheResolver(delegate);
  }

  @NotNull
  public static Resolver createJarResolver(@NotNull ZipFile jarFile) throws IOException {
    return new SoftJarResolver(jarFile);
  }

  @NotNull
  public static Resolver getEmptyResolver() {
    return EmptyResolver.INSTANCE;
  }

  /**
   * Returns a class-file node
   *
   * @param className class name in <i>binary</i> form (see JVM specification)
   * @return class-file for accessing bytecode
   */
  @Nullable
  public abstract ClassNode findClass(@NotNull String className);

  /**
   * Returns actual class holder which contains a specified class.
   * <p>
   * e.g.: if {@code this} is a containing-resolver (it has inner resolvers), then the method will return the innermost
   * resolver which really contains a class
   * <p>
   * it is meant to use in the following use-case: given some über-jar we may request the very .jar-file resolver from
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
