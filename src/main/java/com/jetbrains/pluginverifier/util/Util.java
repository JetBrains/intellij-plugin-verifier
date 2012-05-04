package com.jetbrains.pluginverifier.util;

import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.pool.ContainerClassPool;
import com.jetbrains.pluginverifier.pool.JarClassPool;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class Util {
  public static void fail(String message) {
    System.err.println(message);
    System.exit(1);
  }

  public static List<JarFile> getJars(File directory) throws IOException {
    final File[] jars = directory.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(final File dir, final String name) {
        return name.toLowerCase().endsWith(".jar");
      }
    });

    List<JarFile> jarFiles = new ArrayList<JarFile>(jars.length);
    for (File jar : jars) {
      jarFiles.add(new JarFile(jar, false));
    }

    return jarFiles;
  }

  public static ClassPool makeClassPool(String moniker, List<JarFile> jars) throws IOException {
    List<ClassPool> pool = new ArrayList<ClassPool>();

    for (JarFile jar : jars) {
      pool.add(new JarClassPool(jar));
    }

    return new ContainerClassPool(moniker, pool);
  }
}
