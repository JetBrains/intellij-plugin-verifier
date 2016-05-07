package com.intellij.structure.resolvers;


import com.intellij.structure.domain.Plugin;
import com.intellij.structure.errors.IncorrectPluginException;
import com.intellij.structure.impl.resolvers.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Provides an access to the byte-code of a class by its name via the {@link #findClass(String)}.
 * Note that the way of constructing the {@code Resolver} affects the searching order
 * (it is similar to the Java <i>class-path</i> option).
 */
public abstract class Resolver implements Closeable {

  /**
   * Creates a resolver for the given plugin.
   * <p>It consists of all the plugin classes and .jar libraries.</p>
   * <p>Note: the resolver may firstly extract the plugin in temporary directory for performance reasons.
   * So it's necessary to invoke {@link Resolver#close()} after Resolver is no longer needed. It will clean the disk space.</p>
   *
   * @param plugin plugin for which resolver should be created
   * @return resolver for the specified plugin
   * @throws IOException if disk error occurs during attempt to read a class-file or to extract a plugin
   * @throws IncorrectPluginException if the plugin has broken class-files or it has an incorrect directories structure
   */
  @NotNull
  public static Resolver createPluginResolver(@NotNull Plugin plugin) throws IncorrectPluginException, IOException {
    return PluginResolver.createPluginResolver(plugin);
  }

  /**
   * Creates a resolver which combines the specified list of resolvers similar to the <i>Java class-path</i> setting.
   * During the class search attempt the list will be searched in the left-to-right order until the class is found.
   *
   * @param presentableName some name determining the union resolver name (it can be obtained via the {@link #toString()})
   * @param resolvers list of the resolvers according to the class-path order
   * @return a combining resolver
   */
  @NotNull
  public static Resolver createUnionResolver(@NotNull String presentableName, @NotNull List<Resolver> resolvers) {
    return ContainerResolver.createFromList(presentableName, resolvers);
  }

  /**
   * Creates a resolver designated to cache the sought-for classes. It may be used for performance reasons.
   *
   * @param delegate a resolver to which class search attempts will be delegated
   * @return a caching resolver
   */
  @NotNull
  public static Resolver createCacheResolver(@NotNull Resolver delegate) {
    return new CacheResolver(delegate);
  }

  /**
   * Creates a resolver for the given jar-file (a .jar archive containing the class-files)
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

  /**
   * Returns the resolver which doesn't contain any class.
   *
   * @return an empty resolver
   */
  @NotNull
  public static Resolver getEmptyResolver() {
    return EmptyResolver.INSTANCE;
  }

  /**
   * Returns a class-file node with the specified name. If {@code this} resolver contains multiple
   * instances of classes with the same name the first one in the <i>class-path</i> order will be returned.
   *
   * @param className class name in <i>binary</i> form (see JVM specification)
   * @return a class-node for accessing the bytecode
   * @throws IOException if IO error occurs, e.g. file .class-file was deleted
   */
  @Nullable
  public abstract ClassNode findClass(@NotNull String className) throws IOException;

  /**
   * Returns the resolver which contains the given class: invocation of {@link #findClass(String)} on the
   * resulting {@code Resolver} returns the same instance of the class-node as an invocation of {@link #findClass(String)}
   * on {@code this} instance.
   *
   * @param className class name for which resolver should be found (in <i>binary</i> form)
   * @return actual class resolver or {@code null} if {@code this} resolver doesn't contain a specified class
   */
  @Nullable
  public abstract Resolver getClassLocation(@NotNull String className);

  /**
   * Returns the <i>binary</i> names of all the contained classes.
   *
   * @return all the classes names in the <i>binary</i> form.
   */
  @NotNull
  public abstract Set<String> getAllClasses();


  /**
   * Checks whether this resolver contains any class. Classes can be obtained through {@link #getAllClasses()}.
   *
   * @return true if this resolver is not empty, false otherwise
   */
  public abstract boolean isEmpty();

  @Override
  public void close() {
    //doesn't throw an IOException
  }
}
