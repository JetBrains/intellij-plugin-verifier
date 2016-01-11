package com.intellij.structure.domain;

import com.intellij.structure.pool.ClassPool;
import org.jetbrains.annotations.NotNull;

/**
 * Set of class files necessary for running IDE (JDK classes for example)
 *
 * @author Sergey Patrikeev
 */
public interface IdeRuntime {

  /**
   * Returns class pool of this ide runtime (JDK) instance, i.e.
   * all its class-files accessor
   *
   * @return class pool for accessing containing classes
   */
  @NotNull
  ClassPool getClassPool();

}
