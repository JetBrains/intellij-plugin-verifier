package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.io.Files;
import com.intellij.structure.domain.*;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.CommandHolder;
import com.jetbrains.pluginverifier.PluginVerifierOptions;
import com.jetbrains.pluginverifier.VerificationContextImpl;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.VerificationProblem;
import com.jetbrains.pluginverifier.repository.RepositoryManager;
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
import org.jetbrains.annotations.Nullable;
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
  private static List<Pair<UpdateInfo, File>> loadPluginFiles(@NotNull String pluginToTestArg, @NotNull IdeVersion ideVersion) {
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
        File update = RepositoryManager.getInstance().getPluginFile(updateInfo);
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
  private static List<Pair<UpdateInfo, File>> fetchPlugins(@NotNull IdeVersion ideVersion, @NotNull File pluginListFile, @NotNull List<String> pluginPaths) {
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
    int idx = name.lastIndexOf('.');
    if (idx != -1) {
      name = name.substring(0, idx);
    }
    if (name.matches("\\d+")) {
      return new UpdateInfo(Integer.parseInt(name));
    }
    return new UpdateInfo(name, name, "?");
  }

  @NotNull
  private static List<Pair<UpdateInfo, File>> downloadPluginBuilds(@NotNull String pluginId, @NotNull IdeVersion ideVersion) {
    List<UpdateInfo> compatibleUpdatesForPlugins;
    try {
      compatibleUpdatesForPlugins = RepositoryManager.getInstance().getAllCompatibleUpdatesOfPlugin(ideVersion, pluginId);
    } catch (IOException e) {
      throw FailUtil.fail("Failed to fetch list of " + pluginId + " versions", e);
    }

    List<Pair<UpdateInfo, File>> result = new ArrayList<Pair<UpdateInfo, File>>();
    for (UpdateInfo updateInfo : compatibleUpdatesForPlugins) {
      try {
        result.add(Pair.create(updateInfo, RepositoryManager.getInstance().getPluginFile(updateInfo)));
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

    Jdk javaRuntime = createJdk(commandLine);

    PluginVerifierOptions options = PluginVerifierOptions.parseOpts(commandLine);

    long startTime = System.currentTimeMillis();

    //updateInfo -> (IDEA-build -> Problems)
    final Map<UpdateInfo, Map<String, ProblemSet>> results = new HashMap<UpdateInfo, Map<String, ProblemSet>>();

    TeamCityLog log = TeamCityLog.getInstance(commandLine);


    List<Pair<UpdateInfo, ? extends Problem>> brokenPlugins = new ArrayList<Pair<UpdateInfo, ? extends Problem>>();

    for (int i = 1; i < freeArgs.size(); i++) {
      File ideaDirectory = new File(freeArgs.get(i));
      verifyIdeaDirectory(ideaDirectory);

      Ide ide = IdeManager.getInstance().createIde(ideaDirectory);


      List<Pair<UpdateInfo, File>> pluginFiles = loadPluginFiles(pluginsToTestArg, ide.getVersion());

      for (Pair<UpdateInfo, File> pluginFile : pluginFiles) {
        try {
          Plugin plugin = PluginManager.getInstance().createPlugin(pluginFile.getSecond());

          ProblemSet problemSet = verifyPlugin(ide, javaRuntime, getExternalClassPath(commandLine), plugin, options, log);

          final UpdateInfo updateInfo = new UpdateInfo(plugin.getPluginId(), plugin.getPluginName(), plugin.getPluginVersion());


          Map<String, ProblemSet> ideaToProblems = results.get(updateInfo);
          if (ideaToProblems == null) {
            ideaToProblems = new HashMap<String, ProblemSet>();
            results.put(updateInfo, ideaToProblems);
          }

          ideaToProblems.put(ide.getVersion().asString(), problemSet);

          ide = ide.getExpandedIde(plugin);

        } catch (Exception e) {

          String localizedMessage = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getClass().getName();
          brokenPlugins.add(Pair.create(pluginFile.getFirst(), new VerificationProblem(localizedMessage, pluginFile.getFirst().toString())));

          final String message = "failed to verify plugin " + pluginFile.getFirst();
          System.err.println(message);
          e.printStackTrace();
          log.messageError(message, Util.getStackTrace(e));
        }
      }

    }

    System.out.println("Plugin verification took " + (System.currentTimeMillis() - startTime) + "ms");

    saveReportToFile(commandLine, results);


    Map<UpdateInfo, ProblemSet> pluginsProblems = mergeIdeResults(results);
    appendBrokenPluginProblems(pluginsProblems, brokenPlugins);

    TeamCityUtil.printTeamCityProblems(log, pluginsProblems, Predicates.alwaysTrue(), TeamCityUtil.ReportGrouping.parseGrouping(commandLine));

    final int problemsCnt = countTotalProblems(pluginsProblems);

    boolean hasProblems = problemsCnt > 0;
    System.out.println("Plugin compatibility " + (hasProblems ? "FAILED" : "OK"));
    if (hasProblems) {
      log.buildStatus(problemsCnt > 1 ? problemsCnt + " problems" : "1 problem");
    }

    return hasProblems ? 2 : 0;
  }

  private int countTotalProblems(Map<UpdateInfo, ProblemSet> problems) {
    Set<Problem> set = new HashSet<Problem>();
    for (ProblemSet problemSet : problems.values()) {
      set.addAll(problemSet.getAllProblems());
    }
    return set.size();
  }

  @NotNull
  private Map<UpdateInfo, ProblemSet> mergeIdeResults(@NotNull Map<UpdateInfo, Map<String, ProblemSet>> idesResults) {
    Map<UpdateInfo, ProblemSet> result = new HashMap<UpdateInfo, ProblemSet>();

    //update --> (idea --> problems) ===> (update --> merged problems)
    for (Map.Entry<UpdateInfo, Map<String, ProblemSet>> entry : idesResults.entrySet()) {
      UpdateInfo info = entry.getKey();

      ProblemSet set = result.get(info);
      if (set == null) {
        set = new ProblemSet();
        result.put(info, set);
      }

      for (Map.Entry<String, ProblemSet> setEntry : entry.getValue().entrySet()) {
        set.appendProblems(setEntry.getValue());
      }
    }
    return result;
  }

  private void appendBrokenPluginProblems(@NotNull Map<UpdateInfo, ProblemSet> problems,
                                          @NotNull List<Pair<UpdateInfo, ? extends Problem>> brokenPlugins) {
    for (Pair<UpdateInfo, ? extends Problem> brokenPlugin : brokenPlugins) {
      ProblemSet set = problems.get(brokenPlugin.getFirst());
      if (set == null) {
        set = new ProblemSet();
        problems.put(brokenPlugin.getFirst(), set);
      }

      String id = brokenPlugin.getFirst().getPluginId();
      if (id == null) {
        id = brokenPlugin.getFirst().getPluginName();
      }
      set.addProblem(brokenPlugin.getSecond(), ProblemLocation.fromPlugin(id));
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
   * @return problems of plugin against specified IDEA
   */
  @NotNull
  private ProblemSet verifyPlugin(@NotNull Ide ide,
                                  @NotNull Jdk jdk,
                                  @Nullable Resolver externalClassPath,
                                  @NotNull Plugin plugin,
                                  @NotNull PluginVerifierOptions options,
                                  @NotNull TeamCityLog log) throws IOException {

    String message = "Verifying " + plugin.getPluginId() + ":" + plugin.getPluginVersion() + " against " + ide.getVersion() + "... ";
    System.out.print(message);
    log.message(message);

    VerificationContextImpl ctx = new VerificationContextImpl(plugin, ide, jdk, externalClassPath, options);

    TeamCityLog.Block block = log.blockOpen(plugin.getPluginId());

    try {

      Verifiers.processAllVerifiers(ctx);

      ProblemSet problemSet = ctx.getProblemSet();

      printProblemsOnStdout(problemSet);

      //for test only purposes
      myLastProblemSet = problemSet;

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
