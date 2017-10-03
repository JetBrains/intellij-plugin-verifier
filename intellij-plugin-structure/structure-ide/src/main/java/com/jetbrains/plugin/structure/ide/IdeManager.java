package com.jetbrains.plugin.structure.ide;

import com.jetbrains.plugin.structure.base.logging.Logger;
import com.jetbrains.plugin.structure.base.logging.LoggerFactory;
import com.jetbrains.plugin.structure.intellij.version.IdeVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Factory class which creates the {@link Ide} instances.
 */
public abstract class IdeManager {

  @NotNull
  public static IdeManager createManager() {
    return createManager(LoggerFactory.INSTANCE.createDefaultLogger(IdeManager.class));
  }

  @NotNull
  public static IdeManager createManager(@NotNull Logger logger) {
    return new IdeManagerImpl(logger);
  }

  /**
   * Creates the {@code IDE} instance from the specified directory. IDE may be in the distribution form (a set of .jar
   * files) or in the source code form with the compiled classes.
   *
   * @param idePath an IDE home directory
   * @return created IDE instance
   * @throws IOException if io-error occurs
   */
  @NotNull
  public abstract Ide createIde(@NotNull File idePath) throws IOException;


  /**
   * Similar to the {@link #createIde(File)} but updates a version of the created IDE to the specified one. By default
   * the version of the IDE is read from the 'build.txt'.
   *
   * @param idePath IDE home directory
   * @param version version of the IDE
   * @return created IDE instance
   * @throws IOException if io-error occurs
   */
  @NotNull
  public abstract Ide createIde(@NotNull File idePath, @Nullable IdeVersion version) throws IOException;

}
