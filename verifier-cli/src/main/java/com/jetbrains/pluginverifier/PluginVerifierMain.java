package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.domain.Idea;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.domain.JDK;
import com.jetbrains.pluginverifier.util.*;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.*;
import org.jdom.JDOMException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PluginVerifierMain {

  private static JDK createJdk(CommandLine commandLine) throws IOException {
    File runtimeDirectory;

    if (commandLine.hasOption('r')) {
      runtimeDirectory = new File(commandLine.getOptionValue('r'));
      if (!runtimeDirectory.isDirectory()) {
        Util.fail("Specified runtime directory is not a directory: " + commandLine.getOptionValue('r'));
      }
    }
    else {
      String javaHome = System.getenv("JAVA_HOME");
      if (javaHome == null) {
        Util.fail("JAVA_HOME is not specified");
      }

      runtimeDirectory = new File(javaHome);
      if (!runtimeDirectory.isDirectory()) {
        Util.fail("Invalid JAVA_HOME: " + javaHome);
      }
    }

    return new JDK(runtimeDirectory);
  }

  private static void checkIde(CommandLine commandLine) throws IOException, JDOMException {
    String[] freeArgs = commandLine.getArgs();

    if (freeArgs.length == 0) {
      Util.fail("You have to specify IDE to check");
    }

    JDK jdk = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    for (String ideHome : freeArgs) {
      File ideToCheck = new File(ideHome);
      if (!ideToCheck.isDirectory()) {
        Util.fail("IDE home is not a directory: " + ideToCheck);
      }

      Idea idea = new Idea(ideToCheck, jdk);
      IdeVerifier.verifyIde(idea, options);
    }
  }

  public static void main(String[] args) throws Exception {
    CommandLine commandLine;
    try {
      commandLine = new GnuParser().parse(Util.CMD_OPTIONS, args);
    }
    catch (ParseException e) {
      System.out.println(e.getLocalizedMessage());
      return;
    }

    long start = System.currentTimeMillis();

    if (commandLine.hasOption('a')) {
      checkIde(commandLine);
      return;
    }

    String[] freeArgs = commandLine.getArgs();

    if (commandLine.hasOption('c')) {
      if (freeArgs.length == 0) {
        Util.fail("Build number to compare is not specified");
      }

      if (freeArgs.length != 2) {
        Util.fail("Invalid command arguments\nUsage: validator -c CURRENT_BUILD_NUMBER PREVIOUS_BUILD_NUMBER");
      }

      ProblemListComparator.compare(freeArgs[0], freeArgs[1]);

      return;
    }

    if (commandLine.hasOption('h') || freeArgs.length == 0) {
      Util.printHelp();
      return;
    }

    if (freeArgs.length == 1) {
      Util.failWithHelp("No IDEA directories specified");
    }

    JDK jdk = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    String pluginToTest = freeArgs[0];

    List<IdeaPlugin> pluginsToVerify = new ArrayList<IdeaPlugin>();

    for (int i = 1; i < freeArgs.length; i++) {
      File ideaDirectory = new File(freeArgs[i]);

      if (!ideaDirectory.isDirectory()) {
        Util.fail("Input directory is not found: " + ideaDirectory);
      }

      Idea idea = new Idea(ideaDirectory, jdk);

      IdeaPlugin ideaPlugin;
      if (pluginToTest.matches("\\#\\d+")) {
        File update = DownloadUtils.getUpdate(Integer.parseInt(pluginToTest.substring(1)));
        ideaPlugin = IdeaPlugin.createFromZip(idea, update);
      }
      else {
        ideaPlugin = JarDiscovery.createIdeaPlugin(new File(pluginToTest), idea);
      }

      pluginsToVerify.add(ideaPlugin);
    }

    System.out.println("Reading directories took " + (System.currentTimeMillis() - start) + "ms");

    verifyPluginList(pluginsToVerify, options);
  }

  private static void verifyPluginList(List<IdeaPlugin> plugins, PluginVerifierOptions options) {
    long start = System.currentTimeMillis();

    boolean hasError = false;

    for (IdeaPlugin plugin : plugins) {
      System.out.println("Verifying " + plugin.getId() + " against " + plugin.getIdea().getMoniker());

      ProblemsCollector collector = new ProblemsCollector();
      Verifiers.processAllVerifiers(plugin, options, collector);

      for (Problem problem : collector.getProblems()) {
        hasError = true;
        System.out.println(problem.getDescription());
      }
    }

    System.out.println("Plugin verification took " + (System.currentTimeMillis() - start) + "ms");
    System.out.println(hasError ? "FAILED" : "OK");
    System.exit(hasError ? 1 : 0);
  }

}
