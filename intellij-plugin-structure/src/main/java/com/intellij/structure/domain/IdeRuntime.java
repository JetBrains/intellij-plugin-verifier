package com.intellij.structure.domain;

import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

/**
 * @author Sergey Patrikeev
 */
public interface IdeRuntime {

  @NotNull
  Resolver getResolver();

}
