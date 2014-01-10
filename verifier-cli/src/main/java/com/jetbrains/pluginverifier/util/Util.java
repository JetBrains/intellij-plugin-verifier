package com.jetbrains.pluginverifier.util;

import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.pool.ContainerClassPool;
import com.jetbrains.pluginverifier.pool.JarClassPool;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class Util {

  public static final Options CMD_OPTIONS = new Options()
    .addOption("h", "help", false, "Show help")
    .addOption("r", "runtime", true, "Path to directory containing Java runtime jars (usually rt.jar and tools.jar is sufficient)")
    .addOption("s", "skip-class-for-dup-check", true, "Class name prefixes to skip in duplicate classes check, delimited by ':'")
    .addOption("e", "external-classes", true, "Classes from external libraries. Error will not be reported if class not found. Delimited by ':'")
    .addOption("all", "check-all-plugins-with-ide", false, "Check IDE build with all compatible plugins")
    .addOption("pl", "plugin-list", true, "List of plugin id to check with IDE, delimited by ':'")
    .addOption("ebf", "excluded-build-file", true, "File with list of excluded plugin builds.")
    .addOption("cp", "external-class-path", true, "External class path");

  public static RuntimeException fail(String message) {
    System.err.println(message);
    System.exit(1);
    return new RuntimeException();
  }

  public static void printHelp() {
    new HelpFormatter().printHelp("java -jar verifier.jar <command> [<args>]", CMD_OPTIONS);
  }

  public static File getValidatorHome() {
    return new File(System.getProperty("user.home") + "/.pluginVerifier");
  }

  public static File getPluginCacheDir() {
    String pluginCacheDir = System.getProperty("verifier.plugin.cache.dir");
    if (pluginCacheDir != null) {
      return new File(pluginCacheDir);
    }

    return new File(getValidatorHome(), "cache");
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

    return ContainerClassPool.union(moniker, pool);
  }
}
