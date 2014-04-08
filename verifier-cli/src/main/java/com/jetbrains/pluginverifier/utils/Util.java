package com.jetbrains.pluginverifier.utils;

import com.google.common.base.Predicate;
import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.pool.ContainerClassPool;
import com.jetbrains.pluginverifier.pool.JarClassPool;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;

public class Util {

  public static final Options CMD_OPTIONS = new Options()
    .addOption("h", "help", false, "Show help")
    .addOption("r", "runtime", true, "Path to directory containing Java runtime jars (usually rt.jar and tools.jar is sufficient)")
    .addOption("s", "skip-class-for-dup-check", true, "Class name prefixes to skip in duplicate classes check, delimited by ':'")
    .addOption("e", "external-classes", true, "Classes from external libraries. Error will not be reported if class not found. Delimited by ':'")
    .addOption("all", "check-all-plugins-with-ide", false, "Check IDE build with all compatible plugins")
    .addOption("p", "plugin-to-check", true, "A plugin id to check with IDE, plugin verifier will check ALL compatible plugin builds")
    .addOption("u", "update-to-check", true, "A plugin id to check with IDE, plugin verifier will check LAST plugin build only")
    .addOption("iv", "ide-version", true, "Version of IDE that will be tested, e.g. IU-133.439")
    .addOption("epf", "excluded-plugin-file", true, "File with list of excluded plugin builds.")
    .addOption("d", "dump-broken-plugin-list", true, "File to dump broken plugin list.")
    .addOption("report", "make-report", true, "Create a detailed report about broken plugins.")
    .addOption("xr", "save-results-xml", true, "Save results to xml file")
    .addOption("tc", "team-city-output", false, "Print TeamCity compatible output.")
    .addOption("pluginsFile", "plugins-to-check-file", true, "The file that contains list of plugins to check.")
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
    String pluginCacheDir = Configuration.getInstance().getPluginCacheDir();
    if (pluginCacheDir != null) {
      return new File(pluginCacheDir);
    }

    return new File(getValidatorHome(), "cache");
  }

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
        }
        catch (IOException e) {
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

    return ContainerClassPool.union(moniker, pool);
  }

  public static <T> List<T> concat(Collection<T> first, Collection<T> second) {
    List<T> res = new ArrayList<T>(first.size() + second.size());
    res.addAll(first);
    res.addAll(second);
    return res;
  }
}
