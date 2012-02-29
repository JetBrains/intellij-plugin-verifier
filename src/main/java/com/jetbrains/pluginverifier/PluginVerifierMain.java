package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.pool.ContainerClassPool;
import com.jetbrains.pluginverifier.pool.JarClassPool;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;


/**
 * @author Dennis.Ushakov
 */
public class PluginVerifierMain {
  public static void main(String[] args) throws Exception {
    long start = System.currentTimeMillis();
    final Verifier[] verifiers = parseOpts(args);
    System.out.println("Reading directories took " + (System.currentTimeMillis() - start) + "ms");
    start = System.currentTimeMillis();

    boolean failed = false;
    for (Verifier verifier : verifiers) {
      verifier.verify();
      verifier.dumpErrors();

      if (verifier.hasErrors())
        failed = true;
    }

    System.out.println("Plugin verification took " + (System.currentTimeMillis() - start) + "ms");
    System.out.println(failed ? "FAILED" : "OK");

    System.exit(failed ? 1 : 0);
  }

  private static Options createCommandLineOptions() {
    final Options options = new Options();
    options.addOption("h", "help", false, "Show help");
    options.addOption("r", "runtime", true, "Path to directory containing Java runtime jars (usually rt.jar and tools.jar is sufficient)");
    return options;
  }

  public static Verifier[] parseOpts(final String[] args) throws IOException {
    if (args.length < 2)
      printHelp();

    final CommandLine commandLine;
    try {
      final PosixParser parser = new PosixParser();
      commandLine = parser.parse(createCommandLineOptions(), args);
    } catch (ParseException e) {
      System.err.println("Parsing failed. Reason: " + e.getMessage());
      printHelp();
      return null;
    }

    final String[] freeArgs = commandLine.getArgs();

    if (freeArgs.length < 2) {
      System.err.println("No IDEA directories specified");
      printHelp();
      return null;
    }

    if (!commandLine.hasOption('r')) {
      System.err.println("No runtime specified");
      printHelp();
      return null;
    }

    final File runtimeDirectory = new File(commandLine.getOptionValue('r'));
    if (!runtimeDirectory.isDirectory()) {
      Util.fail("runtime directory is not found");
    }

    final String pluginDir = freeArgs[0];
    final String[] pluginDirs;
    if (pluginDir.startsWith("@")) {
      pluginDirs = readPluginsList(pluginDir);
    } else {
      checkExists("plugin", pluginDir);
      pluginDirs = new String[]{pluginDir};
    }

    final List<ClassPool> ideaPools = new ArrayList<ClassPool>();

    for (int i = 1; i < freeArgs.length; i++) {
      final File ideaDirectory = new File(freeArgs[i]);
      final String moniker = ideaDirectory.getPath();

      if (!ideaDirectory.exists() || !ideaDirectory.isDirectory()) {
        System.err.println("Input directory " + moniker + " is not found");
      }

      ArrayList<ClassPool> jars = new ArrayList<ClassPool>();
      createJarPoolsFromIdeaDir(moniker, ideaDirectory, jars);
      if (jars.size() == 0) {
        Util.fail("Nothing discovered under " + moniker + " directory");
      }

      createJarPoolsFromDirectory(runtimeDirectory.getPath(), runtimeDirectory, jars, true);

      ideaPools.add(new ContainerClassPool(moniker, jars));
    }

    final Verifier[] result = new Verifier[pluginDirs.length];
    for (int i = 0; i < pluginDirs.length; i++) {
      final String dir = pluginDirs[i];
      final ClassPool pool = createPluginPool(dir);
      result[i] = pool != null ? new PluginVerifier(pool, ideaPools) : new BadFileVerifier(dir);
    }

    return result;
  }

  private static String[] readPluginsList(final String pluginDir) throws IOException {
    final BufferedReader reader = new BufferedReader(new FileReader(pluginDir.substring(1)));
    final ArrayList<String> dirs = new ArrayList<String>();
    String line;
    while ((line = reader.readLine()) != null) {
      dirs.add(line);
    }
    reader.close();
    return dirs.toArray(new String[dirs.size()]);
  }

  private static ClassPool createPluginPool(final String pluginDir) {
    try {
      final File pluginDirFile = new File(pluginDir);

      if (!pluginDirFile.exists()) return null;
      if (pluginDir.endsWith(".jar")) {
        return new JarClassPool("plugin " + pluginDir, new JarFile(pluginDir));
      }

      final File realDir;
      if (pluginDir.endsWith(".zip")) {
        realDir = unzip(pluginDirFile);
      } else if (pluginDirFile.isDirectory()) {
        realDir = pluginDirFile;
      } else {
        System.err.println("Unknown input file: " + pluginDirFile);
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
    Unzip.unzip(pluginDir, zip);
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

  private static void createJarPoolsFromIdeaDir(final String name, final File dir, final List<ClassPool> result) throws IOException {
    // lib/*.jar excluding lib/*_rt.jar
    final File[] libfiles = new File(dir, "lib").listFiles();
    if (libfiles == null) {
      Util.fail("No lib directory under " + dir);
      return;
    }

    for (File file : libfiles) {
      if (file.isFile() && file.getName().toLowerCase().endsWith(".jar") && !file.getName().toLowerCase().endsWith("_rt.jar")) {
        System.out.println(name + ": discovered jar " + file);
        result.add(new JarClassPool(name, new JarFile(file)));
      }
    }

    // plugins/*/lib/*.jar
    createJarPoolsFromPluginDir(name, new File(dir, "plugins"), result);
  }

  private static void createJarPoolsFromDirectory(final String name, final File dir, final List<ClassPool> result, boolean recursive) throws IOException {
    final File[] files = dir.listFiles();
    if (files == null)
      return;

    for (File file : files) {
      if (recursive && file.isDirectory()) {
        createJarPoolsFromDirectory(name, file, result, recursive);
      } else if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
        System.out.println(name + ": discovered jar " + file);
        result.add(new JarClassPool(name, new JarFile(file)));
      }
    }
  }

  private static void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("verifier -r RuntimeDirectory PluginZip IdeaDirectory [IdeaDirectory]*", createCommandLineOptions());
    System.exit(1);
  }

  private static void checkExists(final String name, final String dir) {
    if (!(new File(dir)).exists()) {
      Util.fail(name + " " + dir + " doesn't exist");
    }
  }
}
