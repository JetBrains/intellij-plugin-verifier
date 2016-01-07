package com.intellij.structure.resolvers;


import com.intellij.structure.bytecode.ClassFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides access to byte-code of class by its name
 */
public interface Resolver {

  /**
   * Returns class-node of specified .class-file
   *
   * @param className class name in binary form (that is '.' replaced with '/') and
   *                  some other applied rules for naming of anonymous and inner classes.
   * @return byte-code accessor
   */
  @Nullable
  ClassFile findClass(@NotNull String className);

  /**
   * Returns moniker of class within its containing plugin. It may be name of .jar file
   * where this class was found
   *
   * @param className class name in binary form (that is '.' replaced with '/') and
   *                  some other applied rules of naming anonymous and inner classes.
   * @return moniker of this class location
   */
  @Nullable
  String getClassLocationMoniker(@NotNull String className);

}
