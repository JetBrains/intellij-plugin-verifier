package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.jetbrains.pluginverifier.*;
import com.jetbrains.pluginverifier.domain.Idea;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.domain.JDK;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.util.DownloadUtils;
import com.jetbrains.pluginverifier.util.Util;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Default command
 * @author Sergey Evdokimov
 */
public class CheckPlugin extends VerifierCommand {
  public CheckPlugin() {
    super("check-plugin");
  }

  @Override
  public void execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (commandLine.getArgs().length == 0) {
      // it's default command. Looks like user start application without parameters
      throw Util.fail("You must specify one of the commands: " + Joiner.on(", ").join(CommandHolder.getCommandMap().keySet()) + "\n" +
                      "Examples:\n" +
                      "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963 ~/EAPs/idea-IU-129.713 ~/EAPs/idea-IU-133.439\n" +
                      "java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439 -pl org.intellij.scala");
    }

    if (freeArgs.isEmpty()) {
      // User run command 'check-plugin' without parameters
      throw Util.fail("You must specify plugin to check and IDE, example:\n" +
                      "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
                      "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963");
    }

    String pluginToTest = freeArgs.get(0);

    if (pluginToTest.matches("[a-zA-Z0-9\\-]+") && !new File(pluginToTest).exists()) {
      // Looks like user write unknown command. This command was called because it's default command.
      throw Util.fail("Unknown command: " + pluginToTest + "\navailable commands: " + Joiner.on(", ").join(CommandHolder.getCommandMap().keySet()));
    }


    if (freeArgs.size() == 1) {
      throw Util.fail("You must specify IDE directory/directories, example:\n" +
                      "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963");
    }

    JDK jdk = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    List<IdeaPlugin> pluginsToVerify = new ArrayList<IdeaPlugin>();

    for (int i = 1; i < freeArgs.size(); i++) {
      File ideaDirectory = new File(freeArgs.get(i));

      if (!ideaDirectory.exists()) {
        throw Util.fail("IDE directory is not found: " + ideaDirectory);
      }

      if (!ideaDirectory.isDirectory()) {
        throw Util.fail("IDE directory is not a directory: " + ideaDirectory);
      }

      Idea idea = new Idea(ideaDirectory, jdk, getExternalClassPath(commandLine));

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

    verifyPluginList(pluginsToVerify, options);
  }

  private static void verifyPluginList(List<IdeaPlugin> plugins, PluginVerifierOptions options) {
    long start = System.currentTimeMillis();

    boolean hasError = false;

    for (IdeaPlugin plugin : plugins) {
      System.out.println("Verifying " + plugin.getId() + " against " + plugin.getIdea().getMoniker());

      VerificationContextImpl ctx = new VerificationContextImpl(options);
      Verifiers.processAllVerifiers(plugin, ctx);

      for (Problem problem : ctx.getProblems()) {
        hasError = true;
        System.out.println(problem.getDescription());
      }
    }

    System.out.println("Plugin verification took " + (System.currentTimeMillis() - start) + "ms");
    System.out.println(hasError ? "FAILED" : "OK");
    System.exit(hasError ? 1 : 0);
  }

}
