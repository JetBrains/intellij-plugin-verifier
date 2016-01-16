package com.intellij.structure.domain;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

/**
 * Set of class files necessary for running IDE (JDK classes for example)
 *
 * @author Sergey Patrikeev
 */
public interface IdeRuntime {

  /**
   * Returns class pool of this ide runtime (JDK) instance, i.e. all its class-files accessor
   *
   * @return class-files accessor
   */
  @NotNull
  Resolver getClassPool();

  /**
   * Returns moniker of this IDE-runtime (it may be for example path to the runtime classes or other string useful for
   * debugging)
   *
   * @return moniker
   */
  @NotNull
  String getMoniker();

}
