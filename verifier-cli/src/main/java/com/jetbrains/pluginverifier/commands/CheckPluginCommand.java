package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeRuntime;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.errors.BrokenPluginException;
import com.intellij.structure.impl.domain.IdeaManager;
import com.intellij.structure.impl.domain.IdeaPluginManager;
import com.jetbrains.pluginverifier.CommandHolder;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContextImpl;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.error.VerificationError;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.VerificationProblem;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
import com.jetbrains.pluginverifier.results.ProblemLocation;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.utils.FailUtil;
import com.jetbrains.pluginverifier.utils.Pair;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import com.jetbrains.pluginverifier.utils.Util;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityLog;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityUtil;
import com.jetbrains.pluginverifier.verifiers.Verifiers;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.*;

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
  private static List<Pair<UpdateInfo, File>> loadPluginFiles(@NotNull String pluginToTestArg, @NotNull String ideVersion) {
    if (pluginToTestArg.startsWith("@")) {
      File pluginListFile = new File(pluginToTestArg.substring(1));
      List<String> pluginPaths;
      try {
        pluginPaths = Files.readLines(pluginListFile, Charsets.UTF_8);
      } catch (IOException e) {
        throw FailUtil.fail("Cannot load plugins from " + pluginListFile.getAbsolutePath() + ": " + e.getMessage(), e);
      }
      return fetchPlugins(ideVersion, pluginListFile, pluginPaths);

    } else if (pluginToTestArg.matches("#\\d+")) {
      String pluginId = pluginToTestArg.substring(1);
      try {
        int updateId = Integer.parseInt(pluginId);
        UpdateInfo updateInfo = RepositoryManager.getInstance().findUpdateById(updateId);
        File update = RepositoryManager.getInstance().getOrLoadUpdate(updateInfo);
        return Collections.singletonList(Pair.create(new UpdateInfo(updateId), update));
      } catch (IOException e) {
        throw FailUtil.fail("Cannot load plugin #" + pluginId, e);
      }
    } else {
      File file = new File(pluginToTestArg);
      if (!file.exists()) {
        // Looks like user write unknown command. This command was called because it's default command.
        throw FailUtil.fail("Unknown command: " + pluginToTestArg + "\navailable commands: " + Joiner.on(", ").join(CommandHolder.getCommandMap().keySet()));
      }
      return Collections.singletonList(Pair.create(updateInfoByFile(file), file));
    }
  }

  @NotNull
  private static List<Pair<UpdateInfo, File>> fetchPlugins(@NotNull String ideVersion, @NotNull File pluginListFile, @NotNull List<String> pluginPaths) {
    List<Pair<UpdateInfo, File>> pluginsFiles = new ArrayList<Pair<UpdateInfo, File>>();

    for (String pluginPath : pluginPaths) {
      pluginPath = pluginPath.trim();
      if (pluginPath.isEmpty()) continue;

      if (pluginPath.startsWith("id:")) {
        //single plugin by plugin build number

        String pluginId = pluginPath.substring("id:".length());
        List<Pair<UpdateInfo, File>> pluginBuilds = downloadPluginBuilds(pluginId, ideVersion);
        if (!pluginBuilds.isEmpty()) {
          pluginsFiles.add(pluginBuilds.get(0));
        }

      } else if (pluginPath.startsWith("ids:")) {
        //all updates of this plugin compatible with specified IDEA

        String pluginId = pluginPath.substring("ids:".length());
        pluginsFiles.addAll(downloadPluginBuilds(pluginId, ideVersion));

      } else {
        File file = new File(pluginPath);
        if (!file.isAbsolute()) {
          file = new File(pluginListFile.getParentFile(), pluginPath);
        }
        if (!file.exists()) {
          throw FailUtil.fail("Plugin file '" + pluginPath + "' specified in '" + pluginListFile.getAbsolutePath() + "' doesn't exist");
        }

        pluginsFiles.add(Pair.create(updateInfoByFile(file), file));
      }
    }

    return pluginsFiles;
  }

  @NotNull
  private static UpdateInfo updateInfoByFile(@NotNull File file) {
    String name = file.getName();
    name = name.substring(0, name.lastIndexOf('.'));
    if (name.matches("\\d+")) {
      return new UpdateInfo(Integer.parseInt(name));
    }
    return new UpdateInfo(name, name, "?");
  }

  @NotNull
  private static List<Pair<UpdateInfo, File>> downloadPluginBuilds(@NotNull String pluginId, @NotNull String ideVersion) {
    List<UpdateInfo> compatibleUpdatesForPlugins;
    try {
      compatibleUpdatesForPlugins = RepositoryManager.getInstance().getCompatibleUpdatesForPlugins(ideVersion, Collections.singletonList(pluginId));
    } catch (IOException e) {
      throw FailUtil.fail("Failed to fetch list of " + pluginId + " versions", e);
    }

    List<Pair<UpdateInfo, File>> result = new ArrayList<Pair<UpdateInfo, File>>();
    for (UpdateInfo updateInfo : compatibleUpdatesForPlugins) {
      try {
        result.add(Pair.create(updateInfo, RepositoryManager.getInstance().getOrLoadUpdate(updateInfo)));
      } catch (IOException e) {
        throw FailUtil.fail("Cannot download '" + updateInfo, e);
      }
    }
    return result;
  }

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    verifyArgs(commandLine, freeArgs);

    String pluginsToTestArg = freeArgs.get(0);

    if (freeArgs.size() == 1) {
      throw FailUtil.fail("You must specify IDE directory/directories, example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963");
    }

    IdeRuntime javaRuntime = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    long startTime = System.currentTimeMillis();

    //updateInfo -> (IDEA-build -> Problems)
    final Map<UpdateInfo, Map<String, ProblemSet>> results = new HashMap<UpdateInfo, Map<String, ProblemSet>>();

    TeamCityLog log = TeamCityLog.getInstance(commandLine);


    List<Pair<UpdateInfo, ? extends Problem>> brokenPlugins = new ArrayList<Pair<UpdateInfo, ? extends Problem>>();

    for (int i = 1; i < freeArgs.size(); i++) {
      File ideaDirectory = new File(freeArgs.get(i));
      verifyIdeaDirectory(ideaDirectory);

      Ide ide = IdeaManager.getInstance().createIde(ideaDirectory, javaRuntime, getExternalClassPath(commandLine));


      List<Pair<UpdateInfo, File>> pluginFiles = loadPluginFiles(pluginsToTestArg, ide.getVersion());

      for (Pair<UpdateInfo, File> pluginFile : pluginFiles) {
        try {
          Plugin plugin = IdeaPluginManager.getInstance().createPlugin(pluginFile.getSecond());

          ProblemSet problemSet = verifyPlugin(ide, plugin, options, log);

          final UpdateInfo updateInfo = new UpdateInfo(plugin.getPluginId(), plugin.getPluginName(), plugin.getPluginVersion());


          Map<String, ProblemSet> ideaToProblems = results.get(updateInfo);
          if (ideaToProblems == null) {
            ideaToProblems = new HashMap<String, ProblemSet>();
            results.put(updateInfo, ideaToProblems);
          }

          ideaToProblems.put(ide.getVersion(), problemSet);

        } catch (Exception e) {
          final String message = "failed to verify plugin " + pluginFile.getFirst();

          brokenPlugins.add(Pair.create(pluginFile.getFirst(), new VerificationProblem(e.getLocalizedMessage(), pluginFile.getFirst().toString())));

          System.err.println(message);
          log.messageError(message, Util.getStackTrace(e));
        }
      }

    }

    System.out.println("Plugin verification took " + (System.currentTimeMillis() - startTime) + "ms");

    saveReportToFile(commandLine, results);


    //map of problems without their exact locations inside corresponding plugins
    Multimap<Problem, UpdateInfo> problemsWithoutLocations = dropOutLocations(results);

    appendBrokenPluginProblems(problemsWithoutLocations, brokenPlugins);

    TeamCityUtil.printReport(log, problemsWithoutLocations, TeamCityUtil.ReportGrouping.parseGrouping(commandLine));

    final int problemsCnt = problemsWithoutLocations.size();
    boolean hasProblems = problemsCnt > 0;
    System.out.println("Plugin compatibility " + (hasProblems ? "FAILED" : "OK"));
    if (hasProblems) {
      log.buildStatus(problemsCnt > 1 ? problemsCnt + " problems" : "1 problem");
    }

    return hasProblems ? 2 : 0;
  }

  private void appendBrokenPluginProblems(@NotNull Multimap<Problem, UpdateInfo> problemsWithoutLocations,
                                          @NotNull List<Pair<UpdateInfo, ? extends Problem>> brokenPlugins) {
    for (Pair<UpdateInfo, ? extends Problem> brokenPlugin : brokenPlugins) {
      problemsWithoutLocations.put(brokenPlugin.getSecond(), brokenPlugin.getFirst());
    }
  }

  private void saveReportToFile(@NotNull CommandLine commandLine,
                                @NotNull Map<UpdateInfo, Map<String, ProblemSet>> results) throws IOException {
    if (results.size() != 1) {
      //only one plugin can have report file
      return;
    }

    String resultFile = commandLine.getOptionValue("pcr");
    if (resultFile == null) {
      //no output file specified
      return;
    }
    File file = new File(resultFile);

    UpdateInfo updateInfo = results.keySet().iterator().next();
    Map<String, ProblemSet> problemsMap = results.get(updateInfo);

    ProblemUtils.savePluginCheckResult(file, problemsMap, updateInfo);
  }

  /**
   * Returns map of problems without their exact location inside corresponding plugins
   */
  @NotNull
  private Multimap<Problem, UpdateInfo> dropOutLocations(@NotNull Map<UpdateInfo, Map<String, ProblemSet>> results) {
    Multimap<Problem, UpdateInfo> allProblems = ArrayListMultimap.create();
    for (Map.Entry<UpdateInfo, Map<String, ProblemSet>> entry : results.entrySet()) {
      for (Map.Entry<String, ProblemSet> setEntry : entry.getValue().entrySet()) {
        for (Problem problem : setEntry.getValue().getAllProblems()) {
          allProblems.put(problem, entry.getKey());
        }
      }
    }

    return allProblems;
  }

  /**
   * @return problems of plugin against specified IDEA
   */
  @NotNull
  private ProblemSet verifyPlugin(@NotNull Ide ide,
                                  @NotNull Plugin plugin,
                                  @NotNull PluginVerifierOptions options,
                                  @NotNull TeamCityLog log) throws IOException, BrokenPluginException, VerificationError {

    String message = "Verifying " + plugin.getPluginId() + ":" + plugin.getPluginVersion() + " against " + ide.getVersion() + "... ";
    System.out.print(message);
    log.message(message);

    VerificationContextImpl ctx = new VerificationContextImpl(options, ide);

    TeamCityLog.Block block = log.blockOpen(plugin.getPluginId());

    try {

      //may throw VerificationError
      Verifiers.processAllVerifiers(plugin, ctx);

      ProblemSet problemSet = ctx.getProblems();

      printProblemsOnStdout(problemSet);

      //for test only purposes
      myLastProblemSet = problemSet;
      ide.addCustomPlugin(plugin);

      return problemSet;

    } finally {
      block.close();
    }
  }

  private void printProblemsOnStdout(ProblemSet problemSet) {
    System.out.println(problemSet.isEmpty() ? "is OK" : " has " + problemSet.count() + " errors");
    problemSet.printProblems(System.out, "");

    Set<Problem> allProblems = problemSet.getAllProblems();

    for (Problem problem : allProblems) {
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
      System.err.println(description);
    }
  }

  private void verifyIdeaDirectory(@NotNull File ideaDirectory) {
    if (!ideaDirectory.exists()) {
      throw FailUtil.fail("IDE directory is not found: " + ideaDirectory);
    }

    if (!ideaDirectory.isDirectory()) {
      throw FailUtil.fail("IDE directory is not a directory: " + ideaDirectory);
    }
  }

  private void verifyArgs(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) {
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

  }

  @TestOnly
  public ProblemSet getLastProblemSet() {
    return myLastProblemSet;
  }
}
