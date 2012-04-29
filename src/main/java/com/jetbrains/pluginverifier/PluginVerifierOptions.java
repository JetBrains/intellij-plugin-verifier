package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.domain.Idea;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.domain.JDK;
import com.jetbrains.pluginverifier.util.Util;
import org.apache.commons.cli.*;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PluginVerifierOptions {
  private final IdeaPlugin[] myContexts;
  private final String[] myPrefixesToSkipForDuplicateClassesCheck;

  public PluginVerifierOptions(final IdeaPlugin[] contexts, final String[] prefixesToSkipForDuplicateClassesCheck) {
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

  public static PluginVerifierOptions parseOpts(final String[] args) throws IOException, JDOMException {
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

    final JDK jdk = new JDK(runtimeDirectory);

    final String pluginDir = freeArgs[0];

    final List<IdeaPlugin> result = new ArrayList<IdeaPlugin>();

    for (int i = 1; i < freeArgs.length; i++) {
      final File ideaDirectory = new File(freeArgs[i]);

      if (!ideaDirectory.exists() || !ideaDirectory.isDirectory()) {
        System.err.println("Input directory " + ideaDirectory + " is not found");
      }

      final Idea idea = new Idea(ideaDirectory, jdk);
      final IdeaPlugin ideaPlugin = JarDiscovery.createIdeaPlugin(new File(pluginDir), idea);

      result.add(ideaPlugin);
    }

    final String[] prefixesToSkipDupCheck;

    final String prefixes = commandLine.getOptionValue('s');
    if (prefixes != null && prefixes.length() > 0)
      prefixesToSkipDupCheck = prefixes.replace('.', '/').split(":");
    else
      prefixesToSkipDupCheck = new String[0];

    return new PluginVerifierOptions(result.toArray(new IdeaPlugin[result.size()]), prefixesToSkipDupCheck);
  }

  public static void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("verifier [options] PluginZip IdeaDirectory [IdeaDirectory]*", createCommandLineOptions());
  }

  public IdeaPlugin[] getContexts() {
    return myContexts;
  }

  public String[] getPrefixesToSkipForDuplicateClassesCheck() {
    return myPrefixesToSkipForDuplicateClassesCheck;
  }
}
