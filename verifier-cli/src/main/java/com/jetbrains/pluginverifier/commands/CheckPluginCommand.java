package com.jetbrains.pluginverifier.commands;

import com.intellij.structure.domain.Ide;
import com.intellij.structure.domain.IdeVersion;
import com.intellij.structure.domain.Plugin;
import com.intellij.structure.resolvers.Resolver;
import com.jetbrains.pluginverifier.api.VOptions;
import com.jetbrains.pluginverifier.api.VResults;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.misc.PluginCache;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.utils.Util;
import com.jetbrains.pluginverifier.utils.VerificationProblem;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityLog;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityUtil;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityVPrinter;
import kotlin.Pair;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
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

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (freeArgs.isEmpty()) {
      // User run command 'check-plugin' without parameters
      throw new RuntimeException("You must specify plugin to check and IDE, example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963\n" +
          "java -jar verifier.jar check-plugin #14986 ~/EAPs/idea-IU-117.963");
    }

    String pluginsToTestArg = freeArgs.get(0);

    if (freeArgs.size() == 1) {
      throw new RuntimeException("You must specify IDE directory/directories, example:\n" +
          "java -jar verifier.jar check-plugin ~/work/myPlugin/myPlugin.zip ~/EAPs/idea-IU-117.963");
    }

    VOptions options = VOptions.Companion.parseOpts(commandLine);

    long startTime = System.currentTimeMillis();

    //updateInfo -> (IDEA-build -> Problems)
    final Map<UpdateInfo, Map<IdeVersion, ProblemSet>> results = new HashMap<>();

    TeamCityLog tc = TeamCityLog.Companion.getInstance(commandLine);


    List<Pair<UpdateInfo, ? extends Problem>> brokenPlugins = new ArrayList<>();

    File jdkDir = getJdkDir(commandLine);

    for (int i = 1; i < freeArgs.size(); i++) {
      File ideaDirectory = new File(freeArgs.get(i));
      verifyIdeaDirectory(ideaDirectory);

      Ide ide = createIde(ideaDirectory, commandLine);
      try (Resolver ideResolver = Resolver.createIdeResolver(ide)) {

        List<Pair<UpdateInfo, File>> pluginFiles = Util.INSTANCE.loadPluginFiles(pluginsToTestArg, ide.getVersion());

        for (Pair<UpdateInfo, File> pluginFile : pluginFiles) {
          try {
            Plugin plugin = PluginCache.getInstance().createPlugin(pluginFile.getSecond());

            String text = "Verifying " + plugin.getPluginId() + ":" + plugin.getPluginVersion() + " against " + ide.getVersion() + "... ";
            System.out.print(text);
            tc.message(text);

            ProblemSet problemSet;
            try (TeamCityLog.Block block = tc.blockOpen(plugin.getPluginId())) {
              problemSet = verify(plugin, ide, ideResolver, jdkDir, getExternalClassPath(commandLine), options);
            }

            myLastProblemSet = problemSet;
            System.out.println(problemSet.isEmpty() ? "is OK" : "has " + problemSet.count() + " errors");
            problemSet.printProblems(System.out, "");

            final UpdateInfo updateInfo = new UpdateInfo(plugin.getPluginId(), plugin.getPluginName(), plugin.getPluginVersion());


            Map<IdeVersion, ProblemSet> ideaToProblems = results.get(updateInfo);
            if (ideaToProblems == null) {
              ideaToProblems = new HashMap<>();
              results.put(updateInfo, ideaToProblems);
            }

            ideaToProblems.put(ide.getVersion(), problemSet);

            ide = ide.getExpandedIde(plugin);

          } catch (Exception e) {

            String localizedMessage = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getClass().getName();
            brokenPlugins.add(new Pair<>(pluginFile.getFirst(), new VerificationProblem(localizedMessage, pluginFile.getFirst().toString())));

            final String message = "failed to verify plugin " + pluginFile.getFirst();
            System.err.println(message);
            e.printStackTrace();
            tc.messageError(message, Util.INSTANCE.getStackTrace(e));
          }
        }
      }
    }

    System.out.println("Plugin verification took " + (System.currentTimeMillis() - startTime) + "ms");


    Map<UpdateInfo, ProblemSet> pluginsProblems = mergeIdeResults(results);
    appendBrokenPluginProblems(pluginsProblems, brokenPlugins);

    //TODO: get rid of it.
    IdeVersion someIdeVersion = results.entrySet().iterator().next().getValue().entrySet().iterator().next().getKey();
    VResults vResults = TeamCityUtil.INSTANCE.convertOldResultsToNewResults(pluginsProblems, someIdeVersion);
    new TeamCityVPrinter(tc, TeamCityVPrinter.GroupBy.parse(commandLine)).printResults(vResults);


    final int problemsCnt = countTotalProblems(pluginsProblems);

    boolean hasProblems = problemsCnt > 0;
    System.out.println("Plugin compatibility " + (hasProblems ? "FAILED" : "OK"));
    if (hasProblems) {
      tc.buildStatus(problemsCnt > 1 ? problemsCnt + " problems" : "1 problem");
    }

    return hasProblems ? 2 : 0;
  }

  private int countTotalProblems(Map<UpdateInfo, ProblemSet> problems) {
    Set<Problem> set = new HashSet<>();
    for (ProblemSet problemSet : problems.values()) {
      set.addAll(problemSet.getAllProblems());
    }
    return set.size();
  }

  @NotNull
  private Map<UpdateInfo, ProblemSet> mergeIdeResults(@NotNull Map<UpdateInfo, Map<IdeVersion, ProblemSet>> idesResults) {
    Map<UpdateInfo, ProblemSet> result = new HashMap<>();

    //update --> (idea --> problems) ===> (update --> merged problems)
    for (Map.Entry<UpdateInfo, Map<IdeVersion, ProblemSet>> entry : idesResults.entrySet()) {
      UpdateInfo info = entry.getKey();

      ProblemSet set = result.get(info);
      if (set == null) {
        set = new ProblemSet();
        result.put(info, set);
      }

      for (Map.Entry<IdeVersion, ProblemSet> setEntry : entry.getValue().entrySet()) {
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


  private void verifyIdeaDirectory(@NotNull File ideaDirectory) {
    if (!ideaDirectory.exists()) {
      throw new RuntimeException("IDE directory is not found: " + ideaDirectory);
    }

    if (!ideaDirectory.isDirectory()) {
      throw new RuntimeException("IDE directory is not a directory: " + ideaDirectory);
    }
  }

  @TestOnly
  public ProblemSet getLastProblemSet() {
    return myLastProblemSet;
  }
}
