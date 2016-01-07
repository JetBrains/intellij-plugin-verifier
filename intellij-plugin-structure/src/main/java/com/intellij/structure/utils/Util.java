package com.intellij.structure.utils;

import com.google.common.base.Predicate;
import com.intellij.structure.impl.pool.ContainerClassPool;
import com.intellij.structure.impl.pool.JarClassPool;
import com.intellij.structure.pool.ClassPool;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class Util {

  public static List<JarFile> getJars(File directory, Predicate<File> filter) throws IOException {
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

  public static ClassPool makeClassPool(String moniker, List<JarFile> jars) throws IOException {
    List<ClassPool> pool = new ArrayList<ClassPool>();

    for (JarFile jar : jars) {
      pool.add(new JarClassPool(jar));
    }

    return ContainerClassPool.getUnion(moniker, pool);
  }

}
