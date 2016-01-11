package com.intellij.structure.pool;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * @author Dennis.Ushakov
 */
public interface ClassPool extends Resolver {

  /**
   * @return list of names of all containing classes. Names are present in binary form.
   */
  @NotNull
  Collection<String> getAllClasses();

  boolean isEmpty();
}
