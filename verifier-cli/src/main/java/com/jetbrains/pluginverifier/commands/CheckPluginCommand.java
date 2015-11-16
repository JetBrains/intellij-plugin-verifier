package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.intellij.structure.domain.Idea;
import com.intellij.structure.domain.IdeaPlugin;
import com.intellij.structure.domain.JDK;
import com.jetbrains.pluginverifier.CommandHolder;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContextImpl;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.problems.*;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import com.jetbrains.pluginverifier.utils.TeamCityLog;
import com.jetbrains.pluginverifier.utils.TeamCityUtil;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Default command
 *
 * @author Sergey Evdokimov
 */
public class CheckPluginCommand extends VerifierCommand {
  private ProblemSet myLastProblemSet;

  public CheckPluginCommand() {
    super("check-plugin");
  }

  @NotNull
  private static List<File> loadPluginFiles(@NotNull String pluginToTestArg, @NotNull String ideVersion) {
    if (pluginToTestArg.startsWith("@")) {
      File pluginListFile = new File(pluginToTestArg.substring(1));
      List<String> pluginPaths;
      try {
        pluginPaths = Files.readLines(pluginListFile, Charsets.UTF_8);
      } catch (IOException e) {
        throw FailUtil.fail("Cannot load plugins from " + pluginListFile.getAbsolutePath() + ": " + e.getMessage(), e);
      }
      List<File> pluginsFiles = new ArrayList<File>();
      for (String pluginPath : pluginPaths) {
        if (pluginPath.trim().isEmpty()) continue;

        List<File> files;
        if (pluginPath.startsWith("id:")) {
          String pluginId = pluginPath.substring("id:".length());
          files = Collections.singletonList(downloadPluginBuilds(pluginId, ideVersion).get(0));
        } else if (pluginPath.startsWith("ids:")) {
          String pluginId = pluginPath.substring("ids:".length());
          files = downloadPluginBuilds(pluginId, ideVersion);
        } else {
          File file = new File(pluginPath);
          if (!file.isAbsolute()) {
            file = new File(pluginListFile.getParentFile(), pluginPath);
          }
          if (!file.exists()) {
            throw FailUtil.fail("Plugin file '" + pluginPath + "' specified in '" + pluginListFile.getAbsolutePath() + "' doesn't exist");
          }
          files = Collections.singletonList(file);
        }
        pluginsFiles.addAll(files);
      }
      return pluginsFiles;
    } else if (pluginToTestArg.matches("#\\d+")) {
      String pluginId = pluginToTestArg.substring(1);
      try {
        UpdateInfo updateInfo = RepositoryManager.getInstance().findUpdateById(Integer.parseInt(pluginId));
        File update = RepositoryManager.getInstance().getOrLoadUpdate(updateInfo);
        return Collections.singletonList(update);
      } catch (IOException e) {
        throw FailUtil.fail("Cannot load plugin '" + pluginId + "': " + e.getMessage(), e);
      }
    } else {
      File file = new File(pluginToTestArg);
      if (!file.exists()) {
        // Looks like user write unknown command. This command was called because it's default command.
        throw FailUtil.fail("Unknown command: " + pluginToTestArg + "\navailable commands: " + Joiner.on(", ").join(CommandHolder.getCommandMap().keySet()));
      }
      return Collections.singletonList(file);
    }
  }

  @NotNull
  private static List<File> downloadPluginBuilds(@NotNull String pluginId, @NotNull String ideVersion) {
    List<UpdateInfo> compatibleUpdatesForPlugins;
    try {
      compatibleUpdatesForPlugins = RepositoryManager.getInstance().getCompatibleUpdatesForPlugins(ideVersion, Collections.singletonList(pluginId));
    } catch (IOException e) {
      throw FailUtil.fail("Failed to fetch list of '" + pluginId + "' versions: " + e.getMessage(), e);
    }
    if (compatibleUpdatesForPlugins.isEmpty()) {
      throw FailUtil.fail("No versions of '" + pluginId + " compatible with " + ideVersion + " are found.");
    }
    List<File> result = new ArrayList<File>();
    for (UpdateInfo updateInfo : compatibleUpdatesForPlugins) {
      try {
        result.add(RepositoryManager.getInstance().getOrLoadUpdate(updateInfo));
      } catch (IOException e) {
        throw FailUtil.fail("Cannot download '" + updateInfo + "': " + e.getMessage(), e);
      }
    }
    return result;
  }

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (commandLine.getArgs().length == 0) {
      // it's default command. Looks like user start application without parameters
      throw FailUtil.fail("You must specify one of the commands: " + Joiner.on(", ").join(CommandHolder.getCommandMap().keySet()) + "\n" +
          "Examples:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963 ~/EAPs/idea-IU-129.713 ~/EAPs/idea-IU-133.439\n" +
          "java -jar verifier.jar check-ide ~/EAPs/idea-IU-133.439 -pl org.intellij.scala");
    }

    if (freeArgs.isEmpty()) {
      // User run command 'check-plugin' without parameters
      throw FailUtil.fail("You must specify plugin to check and IDE, example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
          "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963");
    }

    String pluginToTestArg = freeArgs.get(0);

    if (freeArgs.size() == 1) {
      throw FailUtil.fail("You must specify IDE directory/directories, example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963");
    }

    JDK jdk = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    long start = System.currentTimeMillis();

    Multimap<Problem, UpdateInfo> problems = HashMultimap.create();
    TeamCityLog log = TeamCityLog.getInstance(commandLine);
    int problemsCnt = 0;

    for (int i = 1; i < freeArgs.size(); i++) {
      File ideaDirectory = new File(freeArgs.get(i));

      if (!ideaDirectory.exists()) {
        throw FailUtil.fail("IDE directory is not found: " + ideaDirectory);
      }

      if (!ideaDirectory.isDirectory()) {
        throw FailUtil.fail("IDE directory is not a directory: " + ideaDirectory);
      }

      Idea ide = new Idea(ideaDirectory, jdk, getExternalClassPath(commandLine));

      List<File> pluginFiles = loadPluginFiles(pluginToTestArg, ide.getVersion());
      for (File pluginFile : pluginFiles) {
        IdeaPlugin plugin = IdeaPlugin.createIdeaPlugin(pluginFile);

        String message = "Verifying " + plugin.getPluginId() + " against " + ide.getMoniker() + "... ";
        System.out.print(message);

        TeamCityLog.Block block = log.blockOpen(plugin.getPluginId());

        try {
          log.message(message);
          VerificationContextImpl ctx = new VerificationContextImpl(options, ide);

          try {
            Verifiers.processAllVerifiers(plugin, ctx);
          } catch (VerificationError e) {
            System.err.println("Failed to verify plugin " + plugin.getPluginId() + " because " + e.getLocalizedMessage());
            log.messageError("Failed to verify plugin " + plugin.getPluginId() + " because " + e.getLocalizedMessage());
            e.printStackTrace();
          }

          UpdateInfo updateInfo = new UpdateInfo(plugin.getPluginId(), plugin.getPluginName(), plugin.getPluginVersion());

          ProblemSet problemSet = ctx.getProblems();
          System.out.println(problemSet.isEmpty() ? "is OK" : " has " + problemSet.count() + " errors");
          problemSet.printProblems(System.out, "");

          Set<Problem> allProblems = problemSet.getAllProblems();

          for (Problem problem : allProblems) {
            problems.put(problem, updateInfo);

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
            log.buildProblem(description.toString());
          }

          problemsCnt += allProblems.size();

          myLastProblemSet = problemSet;
          ide.addCustomPlugin(plugin);

        } finally {
          block.close();
        }
      }
    }

    TeamCityUtil.printReport(log, problems, TeamCityUtil.ReportGrouping.parseGrouping(commandLine));

    System.out.println("Plugin verification took " + (System.currentTimeMillis() - start) + "ms");
    boolean hasProblems = problemsCnt > 0;
    System.out.println(hasProblems ? "FAILED" : "OK");
    if (hasProblems) {
      log.buildStatus(problemsCnt > 1 ? problemsCnt + " problems" : "1 problem");
    }

    return hasProblems ? 2 : 0;
  }

  @TestOnly
  public ProblemSet getLastProblemSet() {
    return myLastProblemSet;
  }
}
