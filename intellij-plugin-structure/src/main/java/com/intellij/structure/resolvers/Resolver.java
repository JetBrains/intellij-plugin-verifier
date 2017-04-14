package com.intellij.structure.resolvers;


import com.intellij.structure.ide.Ide;
import com.intellij.structure.impl.resolvers.*;
import com.intellij.structure.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * <p>Provides an access to the byte-code of a class by its name via the {@link #findClass(String)}.
 * Note that the way of constructing the {@code Resolver} affects the searching order
 * (it is similar to the Java <i>class-path</i> option)
 * <p>Note: the class is {@code Closeable} thus the {@code Resolver} requires to be closed when it is no longer needed.
 * Some resolvers may extract the classes in the temporary directory for performance reasons, so {@link #close()} will
 * clean the used disk space.</p>
 */
public abstract class Resolver implements Closeable {

  /**
   * Creates a resolver for the given plugin.
   * <p>It consists of all the plugin classes and .jar libraries.</p>
   *
   * @param plugin plugin for which resolver should be created
   * @return resolver for the specified plugin
   * @throws IOException if disk error occurs during attempt to read a class-file or to extract a plugin
   * @throws IllegalArgumentException if the plugin has broken class-files or it has an incorrect directories structure
   */
  @NotNull
  public static Resolver createPluginResolver(@NotNull Plugin plugin) throws IOException {
    return plugin.getOriginalFile() == null ? getEmptyResolver() : PluginResolver.createPluginResolver(plugin.getOriginalFile());
  }

  /**
   * Creates a resolver for the given Ide.
   * <p>If {@code ide} represents a binary IDE distribution the result consists of the .jar files under
   * the <i>{ide.home}/lib</i> directory (not including the subdirectories of <i>lib</i> itself).</p>
   * <p>If {@code ide} represents an IDE compile output the result consists of the class files under the build-directory
   * (for Ultimate it is <i>{ide.home}/out/classes/production</i>)</p>
   *
   * @param ide ide for which to create a resolver
   * @throws IOException if error occurs during attempt to read a class file or an Ide has an incorrect directories structure
   * @return resolver of classes for the given Ide
   */
  @NotNull
  public static Resolver createIdeResolver(@NotNull Ide ide) throws IOException {
    return IdeResolverCreator.createIdeResolver(ide);
  }

  /**
   * Creates a resolver for the given JDK instance.
   * <p>It consists of the jars which are required to run a Java program (<i>rt.jar</i> and others) </p>
   *
   * @param jdkPath path to the JDK or JRE containing directory (on Linux it might be <i>/usr/lib/jvm/java-8-oracle</i>)
   * @return resolver of the JDK classes
   * @throws IOException if IO error occurs during attempt to read a class-file
   */
  @NotNull
  public static Resolver createJdkResolver(@NotNull File jdkPath) throws IOException {
    return JdkResolverCreator.createJdkResolver(jdkPath);
  }

  /**
   * Creates a resolver which combines the specified list of resolvers similar to the <i>Java class-path</i> setting.
   * During the class search attempt the list will be searched in the left-to-right order until the class is found.
   *
   * @param presentableName some name determining the union resolver name (it can be obtained via the {@link #toString()})
   * @param resolvers       list of the resolvers according to the class-path order
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

  /**
   * Returns true if {@code this} Resolver contains the given class. It may be faster
   * than checking {@link #findClass(String)} is not null.
   *
   * @param className class name in <i>binary</i> form (see JVM specification)
   * @return {@code true} if class is in Resolver, {@code false} otherwise
   */
  public abstract boolean containsClass(@NotNull String className);

  /**
   * Returns the roots from which this resolver loads the classes
   *
   * @return roots of the classes
   */
  @NotNull
  public abstract List<File> getClassPath();

}
