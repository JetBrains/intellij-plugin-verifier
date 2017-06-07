package com.intellij.structure.impl.utils;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * @author Sergey Patrikeev
 */
public class FileUtil {

  private static final int TEMP_DIR_ATTEMPTS = 10000;

  public static void closeQuietly(Closeable closeable) {
    try {
      closeable.close();
    } catch (IOException ignored) {
    }
  }

  @NotNull
  private static File getTempDirectory() {
    String tempDir = System.getProperty("intellij.structure.temp.dir");
    if (tempDir != null) {
      return new File(tempDir);
    }
    return FileUtils.getTempDirectory();
  }

  @NotNull
  public static File getExtractedPluginsDirectory() throws IOException {
    final File dir = new File(getTempDirectory(), "extracted-plugins");
    if (!dir.isDirectory()) {
      try {
        if (dir.exists()) {
          FileUtils.forceDelete(dir);
        }
        FileUtils.forceMkdir(dir);
      } catch (IOException e) {
        throw new IOException("Unable to create plugins cache directory " + dir.getAbsoluteFile() + " (check access permissions)", e);
      }
    }
    return dir;
  }

  //it's synchronized because otherwise there is a possibility of two threads creating the same directory
  @NotNull
  public synchronized static File createTempDir(@NotNull File parent, @NotNull String prefix) throws IOException {
    String baseName = prefix + "_" + System.currentTimeMillis();
    IOException lastException = null;
    for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
      File tempDir = new File(parent, baseName + "_" + counter);
      if (!tempDir.exists()) {
        try {
          FileUtils.forceMkdir(tempDir);
          return tempDir;
        } catch (IOException ioe) {
          lastException = ioe;
        }
      }
    }
    throw new IllegalStateException("Failed to create directory under " + parent.getAbsolutePath() + " within "
        + TEMP_DIR_ATTEMPTS + " attempts (tried "
        + baseName + "_0 to " + baseName + "_" + (TEMP_DIR_ATTEMPTS - 1) + ')', lastException);
  }

  private static boolean hasExtension(@NotNull File file, @NotNull String extension) {
    return file.isFile() && extension.equals(Files.getFileExtension(file.getName()));
  }

  public static boolean isJarOrZip(@NotNull File file) {
    return isJar(file) || isZip(file);
  }

  public static boolean isZip(@NotNull File file) {
    return hasExtension(file, "zip");
  }

  public static boolean isJar(@NotNull File file) {
    return hasExtension(file, "jar");
  }
}
