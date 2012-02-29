package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.pool.ContainerClassPool;
import com.jetbrains.pluginverifier.pool.JarClassPool;
import com.jetbrains.pluginverifier.util.Unzip;
import com.jetbrains.pluginverifier.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

public class JarDiscovery {
  public static ClassPool createPluginPool(final File pluginDir) {
    try {
      if (!pluginDir.exists()) return null;
      if (pluginDir.getName().endsWith(".jar")) {
        return new JarClassPool(new JarFile(pluginDir));
      }

      final File realDir;
      if (pluginDir.getName().endsWith(".zip")) {
        realDir = unzip(pluginDir);
      } else if (pluginDir.isDirectory()) {
        realDir = pluginDir;
      } else {
        System.err.println("Unknown input file: " + pluginDir);
        return null;
      }

      List<ClassPool> classPools = createJarPoolsFromPluginDir("plugin", realDir);
      if (classPools.size() == 0) {
        System.err.println("Nothing discovered in plugin folder");
        return null;
      }

      return realDir != null
          ? new ContainerClassPool("plugin " + pluginDir, classPools)
          : null;
    } catch (Exception e) {
      System.err.println("error " + e + " on " + pluginDir);
      return null;
    }
  }

  private static File unzip(final File zipFile) throws IOException {
    final ZipFile zip = new ZipFile(zipFile);

    final File tempDir = Util.createTempDir();
    tempDir.deleteOnExit(); // Yes, I've read why deleteOnExit is evil

    final File pluginDir = new File(tempDir, zipFile.getName());
    assert pluginDir.mkdir();

    System.out.println("Unpacking plugin: " + zipFile.getName());
    Unzip.unzipJars(pluginDir, zip);
    System.out.println("Plugin unpacked to: " + pluginDir);

    return pluginDir;
  }

  private static List<ClassPool> createJarPoolsFromPluginDir(final String name, final File dir) throws IOException {
    ArrayList<ClassPool> result = new ArrayList<ClassPool>();
    createJarPoolsFromPluginDir(name, dir, result);
    return result;
  }


  private static void createJarPoolsFromPluginDir(final String name, final File dir, final List<ClassPool> result) throws IOException {
    // Top-level jars
    createJarPoolsFromDirectory(name, dir, result, false);

    // */lib/*.jar
    final File[] files = dir.listFiles();
    if (files == null)
      return;

    for (File pluginDir : files) {
      if (pluginDir.isDirectory()) {
        createJarPoolsFromDirectory(name, new File(pluginDir, "lib"), result, false);
      }
    }
  }

  public static void createJarPoolsFromIdeaDir(final String name, final File dir, final List<ClassPool> result) throws IOException {
    // lib/*.jar excluding lib/*_rt.jar
    final File[] libfiles = new File(dir, "lib").listFiles();
    if (libfiles == null) {
      Util.fail("No lib directory under " + dir);
      return;
    }

    for (File file : libfiles) {
      if (file.isFile() && file.getName().toLowerCase().endsWith(".jar") && !file.getName().toLowerCase().endsWith("_rt.jar")) {
        System.out.println(name + ": discovered jar " + file);
        result.add(new JarClassPool(new JarFile(file)));
      }
    }

    // plugins/*/lib/*.jar
    createJarPoolsFromPluginDir(name, new File(dir, "plugins"), result);
  }

  public static void createJarPoolsFromDirectory(final String name, final File dir, final List<ClassPool> result, boolean recursive) throws IOException {
    final File[] files = dir.listFiles();
    if (files == null)
      return;

    for (File file : files) {
      if (recursive && file.isDirectory()) {
        createJarPoolsFromDirectory(name, file, result, recursive);
      } else if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
        System.out.println(name + ": discovered jar " + file);
        result.add(new JarClassPool(new JarFile(file)));
      }
    }
  }
}
