package com.intellij.structure.resolvers;


import com.intellij.structure.bytecode.ClassFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides access to byte-code of class by its name
 */
public interface Resolver {

  /**
   * Returns class-file node
   *
   * @param className class name in <i>binary</i> form (that is '.' replaced with '/') and
   *                  some other applied rules for naming of anonymous and inner classes
   * @return class-file for accessing bytecode
   */
  @Nullable
  ClassFile findClass(@NotNull String className);

  /**
   * Get actual class holder which contains a specified class.
   * <p>
   * e.g.: if {@code this} is a containing-resolver (it has inner resolvers),
   * then the method will return <b>some</b> of the innermost resolvers
   * which really contains a class
   * <p>
   * it is meant to use in the following use-case:
   * given some über-jar we may request the very .jar-file resolver
   * for which this class occurred
   *
   * @param className class name for which resolver should be found (in <i>binary</i> form)
   * @return actual class resolver or {@code null} if {@code this} resolver doesn't contain a class
   */
  @Nullable
  Resolver getClassLocation(@NotNull String className);

  /**
   * Get <i>moniker</i> of this resolver
   * <p>
   * It may be for example a name of containing .jar-file
   *
   * @return moniker
   */
  @Nullable
  String getMoniker();
}
