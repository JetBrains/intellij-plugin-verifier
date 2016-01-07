package com.intellij.structure.pool;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  /**
   * @return moniker of this class-pool. It may be for example name of containing .jar-file
   */
  @Nullable
  String getMoniker();

  boolean isEmpty();

}
