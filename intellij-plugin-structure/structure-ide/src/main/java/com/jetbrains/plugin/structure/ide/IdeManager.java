/*
 * Copyright 2000-2020 JetBrains s.r.o. and other contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package com.jetbrains.plugin.structure.ide;

import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Factory class which creates the {@link Ide} instances.
 */
public abstract class IdeManager {

  @NotNull
  public static IdeManager createManager() {
    return new DispatchingIdeManager();
  }

  /**
   * @param idePath Path to IDE
   * @return IDE instance
   * @throws IOException if IO error occurs
   * @throws InvalidIdeException if IDE is invalid
   * @deprecated use {@link #createIde(Path)}
   */
  @NotNull
  @Deprecated
  public final Ide createIde(@NotNull File idePath) throws IOException, InvalidIdeException {
    return createIde(idePath.toPath());
  }

  /**
   * Creates the {@code IDE} instance from the specified directory. IDE may be in the distribution form (a set of .jar
   * files) or in the source code form with the compiled classes.
   *
   * @param idePath an IDE home directory
   * @return created IDE instance
   * @throws IOException         if io-error occurs
   * @throws InvalidIdeException if IDE by specified path is invalid
   */
  @NotNull
  public abstract Ide createIde(@NotNull Path idePath) throws IOException, InvalidIdeException;


  /**
   * @param idePath IDE path
   * @param version IDE version
   * @return IDE instance
   * @throws IOException if IO error occurs
   * @deprecated use {@link #createIde(Path, IdeVersion)}
   */
  @Deprecated
  @NotNull
  public final Ide createIde(@NotNull File idePath, @Nullable IdeVersion version) throws IOException, InvalidIdeException {
    return createIde(idePath.toPath(), version);
  }

  /**
   * Similar to the {@link #createIde(Path)} but updates a version of the created IDE to the specified one. By default
   * the version of the IDE is read from the 'build.txt'.
   *
   * @param idePath IDE home directory
   * @param version version of the IDE
   * @return created IDE instance
   * @throws IOException         if io-error occurs
   * @throws InvalidIdeException if IDE by specified path is invalid
   */
  @NotNull
  public abstract Ide createIde(@NotNull Path idePath, @Nullable IdeVersion version) throws IOException, InvalidIdeException;

}
