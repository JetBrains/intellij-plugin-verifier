package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.jetbrains.pluginverifier.*;
import com.jetbrains.pluginverifier.domain.Idea;
import com.jetbrains.pluginverifier.domain.IdeaPlugin;
import com.jetbrains.pluginverifier.domain.JDK;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.ProblemLocation;
import com.jetbrains.pluginverifier.problems.ProblemSet;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import com.jetbrains.pluginverifier.utils.DownloadUtils;
import com.jetbrains.pluginverifier.utils.StringUtil;
import com.jetbrains.pluginverifier.utils.TeamCityLog;
import com.jetbrains.pluginverifier.utils.Util;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * Default command
 * @author Sergey Evdokimov
 */
public class CheckPluginCommand extends VerifierCommand {
  public CheckPluginCommand() {
    super("check-plugin");
  }

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
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

    IdeaPlugin plugin;
    if (pluginToTest.matches("\\#\\d+")) {
      UpdateInfo updateInfo = RepositoryManager.getInstance().findUpdateById(Integer.parseInt(pluginToTest.substring(1)));
      File update = RepositoryManager.getInstance().getOrLoadUpdate(updateInfo);
      plugin = IdeaPlugin.createFromZip(update);
    }
    else {
      plugin = JarDiscovery.createIdeaPlugin(new File(pluginToTest));
    }

    if (freeArgs.size() == 1) {
      throw Util.fail("You must specify IDE directory/directories, example:\n" +
                      "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963");
    }

    JDK jdk = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    long start = System.currentTimeMillis();

    TeamCityLog tc = TeamCityLog.getInstance(commandLine);
    int problems = 0;
    for (int i = 1; i < freeArgs.size(); i++) {
      File ideaDirectory = new File(freeArgs.get(i));

      if (!ideaDirectory.exists()) {
        throw Util.fail("IDE directory is not found: " + ideaDirectory);
      }

      if (!ideaDirectory.isDirectory()) {
        throw Util.fail("IDE directory is not a directory: " + ideaDirectory);
      }

      Idea idea = new Idea(ideaDirectory, jdk, getExternalClassPath(commandLine));

      String message = "Verifying " + plugin.getId() + " against " + idea.getMoniker() + "... ";
      System.out.print(message);
      TeamCityLog.Block block = tc.blockOpen(message);

      try {
        VerificationContextImpl ctx = new VerificationContextImpl(options, idea);
        Verifiers.processAllVerifiers(plugin, ctx);

        ProblemSet problemSet = ctx.getProblems();
        System.out.println(problemSet.isEmpty() ? "Ok" : problemSet.count() + " errors");
        problemSet.printProblems(System.out, "");
        for (Problem problem : problemSet.getAllProblems()) {
          StringBuilder description = new StringBuilder(problem.getDescription());
          Set<ProblemLocation> locations = problemSet.getLocations(problem);
          if (!locations.isEmpty()) {
            description.append(" at ").append(locations.iterator().next());
            int remaining = locations.size() - 1;
            if (remaining > 0) {
              description.append(" and ").append(remaining).append(" more location");
              if (remaining > 1) description.append("s");
            }
          }
          tc.buildProblem(description.toString());
        }

        problems += problemSet.count();
      }
      finally {
        block.close();
      }
    }

    System.out.println("Plugin verification took " + (System.currentTimeMillis() - start) + "ms");
    boolean hasProblems = problems > 0;
    System.out.println(hasProblems ? "FAILED" : "OK");
    if (hasProblems) {
      tc.buildStatus(problems > 1 ? problems + " problems" : "1 problem");
    }
    return hasProblems ? 2 : 0;
  }


}
