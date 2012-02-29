package com.jetbrains.pluginverifier.util;

import java.io.File;
import java.io.IOException;

public class Util {
  public static void fail(String message) {
    System.err.println(message);
    System.exit(1);
  }

  // From Guava lib
  public static File createTempDir() {
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    String baseName = System.currentTimeMillis() + "-";

    for (int counter = 0; counter < 10000; counter++) {
      File tempDir = new File(baseDir, baseName + counter);
      if (tempDir.mkdir()) {
        return tempDir;
      }
    }

    throw new IllegalStateException("Failed to create directory");
  }

  // From Guava lib
  public static void createParentDirs(File file) throws IOException {
    File parent = file.getCanonicalFile().getParentFile();
    if (parent == null) {
      return;
    }

    parent.mkdirs();
    if (!parent.isDirectory()) {
      throw new IOException("Unable to create parent directories of " + file);
    }
  }
}
