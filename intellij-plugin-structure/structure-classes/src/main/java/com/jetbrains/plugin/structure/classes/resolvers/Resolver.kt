package com.jetbrains.plugin.structure.classes.resolvers

import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.objectweb.asm.tree.ClassNode
import java.io.File
import java.io.IOException
import java.util.Set

/**
 * <p>Provides an access to the byte-code of a class by its name via the {@link #findClass(String)}.
 * Note that the way of constructing the {@code Resolver} affects the searching order
 * (it is similar to the Java <i>class-path</i> option)
 * <p>Note: the class is {@code Closeable} thus the {@code Resolver} requires to be closed when it is no longer needed.
 * Some resolvers may extract the classes in the temporary directory for performance reasons, so {@link #close()} will
 * clean the used disk space.</p>
 */
abstract class Resolver implements Closeable {

  /**
   * Returns a class-file node with the specified name. If {@code this} resolver contains multiple
   * instances of classes with the same name the first one in the <i>class-path</i> order will be returned.
   *
   * @param className class name in <i>binary</i> form (see JVM specification)
   * @return a class-node for accessing the bytecode
   * @throws IOException if IO error occurs, e.g. file .class-file was deleted
   */
  @Nullable
  public abstract ClassNode findClass (@NotNull String className) throws IOException

  /**
   * Returns the resolver which contains the given class: invocation of {@link #findClass(String)} on the
   * resulting {@code Resolver} returns the same instance of the class-node as an invocation of {@link #findClass(String)}
   * on {@code this} instance.
   *
   * @param className class name for which resolver should be found (in <i>binary</i> form)
   * @return actual class resolver or {@code null} if {@code this} resolver doesn't contain a specified class
   */
  @Nullable
  public abstract Resolver getClassLocation (@NotNull String className)

  /**
   * Returns the <i>binary</i> names of all the contained classes.
   *
   * @return all the classes names in the <i>binary</i> form.
   */
  @NotNull
  public abstract Set<String> getAllClasses ()

  /**
   * Checks whether this resolver contains any class. Classes can be obtained through {@link #getAllClasses()}.
   *
   * @return true if this resolver is not empty, false otherwise
   */
  public abstract boolean isEmpty ()

  /**
   * Returns true if {@code this} Resolver contains the given class. It may be faster
   * than checking {@link #findClass(String)} is not null.
   *
   * @param className class name in <i>binary</i> form (see JVM specification)
   * @return {@code true} if class is in Resolver, {@code false} otherwise
   */
  public abstract boolean containsClass (@NotNull String className)

  /**
   * Returns the roots from which this resolver loads the classes
   *
   * @return roots of the classes
   */
  @NotNull
  public abstract List<File> getClassPath ()

  /**
   * Returns the resolvers that actually constitute the given resolver
   *
   * @return final resolvers
   */
  @NotNull
  public abstract List<Resolver> getFinalResolvers ()

}
