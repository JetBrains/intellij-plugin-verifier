package com.intellij.structure.impl.utils;

import com.google.common.base.Predicate;
import com.intellij.structure.resolvers.Resolver;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class JarsUtils {

  @NotNull
  public static List<JarFile> getJars(@NotNull File directory, @NotNull Predicate<File> filter) throws IOException {
    File[] children = directory.listFiles();

    if (children == null) {
      throw new IOException("Failed to read jar directory: " + directory);
    }

    List<JarFile> jarFiles = new ArrayList<JarFile>();

    for (File file : children) {
      if (file.getName().toLowerCase().endsWith(".jar") && filter.apply(file)) {
        try {
          jarFiles.add(new JarFile(file, false));
        } catch (IOException e) {
          System.out.println("Failed to open jar file: " + file + " , " + e.getMessage());
        }
      }
    }

    return jarFiles;
  }

  @NotNull
  public static Resolver makeClassPool(@NotNull String moniker, @NotNull List<JarFile> jars) throws IOException {
    List<Resolver> pool = new ArrayList<Resolver>();

    for (JarFile jar : jars) {
      pool.add(Resolver.createJarClassPool(jar));
    }

    return Resolver.getUnion(moniker, pool);
  }

}
