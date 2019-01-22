package com.jetbrains.plugin.structure.classes.resolvers

import org.objectweb.asm.tree.ClassNode

import java.io.Closeable
import java.io.File
import java.io.IOException

/**
 *
 * Provides an access to the byte-code of a class by its name via the [.findClass].
 * Note that the way of constructing the `Resolver` affects the searching order
 * (it is similar to the Java *class-path* option)
 *
 * Note: the class is `Closeable` thus the `Resolver` requires to be closed when it is no longer needed.
 * Some resolvers may extract the classes in the temporary directory for performance reasons, so [.close] will
 * clean the used disk space.
 */
abstract class Resolver : Closeable {

  /**
   * Read mode used to specify whether this resolver reads [ClassNode]s fully,
   * including methods' code, debug frames, or only classes' signatures.
   */
  enum class ReadMode {
    FULL, SIGNATURES
  }

  /**
   * Read mode this resolved is opened with.
   */
  abstract val readMode: ReadMode

  /**
   * Returns the *binary* names of all the contained classes.
   *
   * @return all the classes names in the *binary* form.
   */
  abstract val allClasses: Set<String>

  /**
   * Returns binary names of all contained packages and their super-packages.
   *
   * For example, if this Resolver contains classes of a package `com/example/utils`
   * then [allPackages] contains `com`, `com/example` and `com/example/utils`.
   *
   * @return names of all packages in *binary* (separated with /) form.
   */
  abstract val allPackages: Set<String>

  /**
   * Checks whether this resolver contains any class. Classes can be obtained through [.getAllClasses].
   *
   * @return true if this resolver is not empty, false otherwise
   */
  abstract val isEmpty: Boolean

  /**
   * Returns the roots from which this resolver loads the classes
   *
   * @return roots of the classes
   */
  abstract val classPath: List<File>

  /**
   * Returns the resolvers that actually constitute the given resolver
   *
   * @return final resolvers
   */
  abstract val finalResolvers: List<Resolver>

  /**
   * Returns a class-file node with the specified name. If `this` resolver contains multiple
   * instances of classes with the same name the first one in the *class-path* order will be returned.
   *
   * @param className class name in *binary* form (see JVM specification)
   * @return a class-node for accessing the bytecode
   * @throws InvalidClassFileException if the class file is not valid from the point of view of the ASM bytecode engineering library
   * @throws IOException if IO error occurs, e.g. file .class-file was deleted
   * @throws InterruptedException if the current thread has been interrupted while searching for the class.
   */
  @Throws(InvalidClassFileException::class, IOException::class, InterruptedException::class)
  abstract fun findClass(className: String): ClassNode?

  /**
   * Returns the resolver which contains the given class: invocation of [.findClass] on the
   * resulting `Resolver` returns the same instance of the class-node as an invocation of [.findClass]
   * on `this` instance.
   *
   * @param className class name for which resolver should be found (in *binary* form)
   * @return actual class resolver or `null` if `this` resolver doesn't contain a specified class
   */
  abstract fun getClassLocation(className: String): Resolver?

  /**
   * Returns true if `this` Resolver contains the given class. It may be faster
   * than checking [.findClass] is not null.
   *
   * @param className class name in *binary* form (see JVM specification)
   * @return `true` if class is in Resolver, `false` otherwise
   */
  abstract fun containsClass(className: String): Boolean

  /**
   * Returns true if `this` Resolver contains the given package,
   * specified with binary name ('/'-separated). It may be faster
   * than fetching [allPackages] and checking for presence in it.
   *
   * @return `true` if this package exists in Resolver, `false` otherwise
   */
  abstract fun containsPackage(packageName: String): Boolean

  /**
   * Runs the given [processor] on [every] [allClasses] class contained in _this_ [Resolver].
   * The [processor] returns `true` to continue processing and `false` to stop.
   *
   * @return `true` if all the classes are processed, and `false` if processing has been stopped.
   * @throws IOException if the processing has failed due to an IO error
   */
  @Throws(IOException::class)
  abstract fun processAllClasses(processor: (ClassNode) -> Boolean): Boolean

}
