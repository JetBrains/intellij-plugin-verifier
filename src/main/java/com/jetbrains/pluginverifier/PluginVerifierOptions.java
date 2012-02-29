package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.pool.ClassPool;
import com.jetbrains.pluginverifier.pool.ContainerClassPool;
import com.jetbrains.pluginverifier.util.*;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PluginVerifierOptions {
  private final VerificationContext[] myContexts;
  private final String[] myPrefixesToSkipForDuplicateClassesCheck;

  public PluginVerifierOptions(final VerificationContext[] contexts, final String[] prefixesToSkipForDuplicateClassesCheck) {
    myContexts = contexts;
    myPrefixesToSkipForDuplicateClassesCheck = prefixesToSkipForDuplicateClassesCheck;
  }

  private static Options createCommandLineOptions() {
    final Options options = new Options();
    options.addOption("h", "help", false, "Show help");
    options.addOption("r", "runtime", true, "Path to directory containing Java runtime jars (usually rt.jar and tools.jar is sufficient)");
    options.addOption("s", "skip-class-for-dup-check", true, "Class name prefixes to skip in duplicate classes check, delimited by ':'");
    return options;
  }

  public static PluginVerifierOptions parseOpts(final String[] args) throws IOException {
    if (args.length < 2) {
      return null;
    }

    final CommandLine commandLine;
    try {
      final Parser parser = new GnuParser();
      commandLine = parser.parse(createCommandLineOptions(), args);
    } catch (ParseException e) {
      System.err.println("Parsing failed. Reason: " + e.getMessage());
      return null;
    }

    final String[] freeArgs = commandLine.getArgs();

    if (freeArgs.length < 2) {
      System.err.println("No IDEA directories specified");
      return null;
    }

    if (!commandLine.hasOption('r')) {
      System.err.println("No runtime specified");
      return null;
    }

    final File runtimeDirectory = new File(commandLine.getOptionValue('r'));
    if (!runtimeDirectory.isDirectory()) {
      Util.fail("runtime directory is not found");
    }

    final String pluginDir = freeArgs[0];

    final List<ClassPool> ideaPools = new ArrayList<ClassPool>();

    for (int i = 1; i < freeArgs.length; i++) {
      final File ideaDirectory = new File(freeArgs[i]);
      final String moniker = ideaDirectory.getPath();

      if (!ideaDirectory.exists() || !ideaDirectory.isDirectory()) {
        System.err.println("Input directory " + moniker + " is not found");
      }

      ArrayList<ClassPool> jars = new ArrayList<ClassPool>();
      JarDiscovery.createJarPoolsFromIdeaDir(moniker, ideaDirectory, jars);
      if (jars.size() == 0) {
        Util.fail("Nothing discovered under " + moniker + " directory");
      }

      JarDiscovery.createJarPoolsFromDirectory(runtimeDirectory.getPath(), runtimeDirectory, jars, true);

      ideaPools.add(new ContainerClassPool(moniker, jars));
    }

    final ClassPool pluginPool = JarDiscovery.createPluginPool(new File(pluginDir));
    if (pluginPool.isEmpty()) {
      Util.fail("No classes discovered in plugin");
    }

    final VerificationContext[] result = new VerificationContext[ideaPools.size()];
    for (int i = 0; i < ideaPools.size(); i++) {
      result[i] = new VerificationContext(pluginPool, ideaPools.get(i));
    }

    final String[] prefixesToSkipDupCheck;

    final String prefixes = commandLine.getOptionValue('s');
    if (prefixes != null && prefixes.length() > 0)
      prefixesToSkipDupCheck = prefixes.replace('.', '/').split(":");
    else
      prefixesToSkipDupCheck = new String[0];

    return new PluginVerifierOptions(result, prefixesToSkipDupCheck);
  }

  public static void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("verifier [options] PluginZip IdeaDirectory [IdeaDirectory]*", createCommandLineOptions());
  }

  public VerificationContext[] getContexts() {
    return myContexts;
  }

  public String[] getPrefixesToSkipForDuplicateClassesCheck() {
    return myPrefixesToSkipForDuplicateClassesCheck;
  }
}
