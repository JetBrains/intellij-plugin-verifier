package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.utils.*;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Sergey Evdokimov
 */
public class NewProblemsCommand extends VerifierCommand {

  public NewProblemsCommand() {
    super("new-problems");
  }

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (freeArgs.isEmpty()) {
      throw Util.fail("You have to specify IDE to check. For example: \"java -jar verifier.jar new-problems ~/EAPs/idea-IU-133.439\"");
    }

    File reportToCheck = new File(freeArgs.get(0));
    if (!reportToCheck.isFile()) {
      throw Util.fail("Report not found: " + reportToCheck);
    }

    ResultsElement checkResult = ProblemUtils.loadProblems(reportToCheck);

    List<String> previousCheckedBuild = findPreviousBuilds(checkResult.getIde());

    if (previousCheckedBuild.isEmpty()) {
      System.out.println("Plugin repository does not contain check result to compare.");
      return 0;
    }

    Multimap<Problem, UpdateInfo> problemsToUpdates = ArrayListMultimap.create();

    for (Map.Entry<UpdateInfo, Collection<Problem>> entry : checkResult.asMap().entrySet()) {
      for (Problem problem : entry.getValue()) {
        problemsToUpdates.put(problem, entry.getKey());
      }
    }

    Multimap<String, Problem> buildToProblems = ArrayListMultimap.create();

    Set<Problem> problems = new HashSet<Problem>(problemsToUpdates.keySet());

    String firstCheckedBuild = previousCheckedBuild.get(0);
    ResultsElement firstBuildResult = ProblemUtils.loadProblems(DownloadUtils.getCheckResult(firstCheckedBuild));

    previousCheckedBuild = previousCheckedBuild.subList(1, previousCheckedBuild.size());

    problems.removeAll(firstBuildResult.getProblems());

    for (String prevBuild : previousCheckedBuild) {
      ResultsElement prevBuildResult = ProblemUtils.loadProblems(DownloadUtils.getCheckResult(prevBuild));

      for (Problem problem : prevBuildResult.getProblems()) {
        if (problems.remove(problem)) {
          buildToProblems.put(prevBuild, problem);
        }
      }
    }

    buildToProblems.putAll("current", problems);

    for (String prevBuild : previousCheckedBuild) {
      Collection<Problem> problemsInBuild = buildToProblems.get(prevBuild);
      if (!problemsInBuild.isEmpty()) {
        System.out.printf("\nIn %s found %d new problems:\n", prevBuild,problemsInBuild.size());

        for (Problem problem : problemsInBuild) {
          System.out.print("    ");
          System.out.println(MessageUtils.cutCommonPackages(problem.getDescription()));
          System.out.println("        in " + Joiner.on(", ").join(problemsToUpdates.get(problem)));
        }
      }
    }

    return 0;
  }

  private static List<String> findPreviousBuilds(String currentBuild) throws IOException {
    List<String> resultsOnInPluginRepository = PRUtil.loadAvailableCheckResultsList();

    Pair<String, Integer> parsedCurrentBuild = parseBuildNumber(currentBuild);

    TreeMap<Integer, String> buildMap = new TreeMap<Integer, String>();

    for (String build : resultsOnInPluginRepository) {
      Pair<String, Integer> pair = parseBuildNumber(build);

      if (parsedCurrentBuild.first.equals(pair.first) && parsedCurrentBuild.second > pair.second) {
        buildMap.put(pair.second, build);
      }
    }

    return new ArrayList<String>(buildMap.values());
  }

  private static Pair<String, Integer> parseBuildNumber(String buildNumber) {
    int idx = buildNumber.lastIndexOf('.');

    return Pair.create(buildNumber.substring(0, idx), Integer.parseInt(buildNumber.substring(idx + 1)));
  }
}
