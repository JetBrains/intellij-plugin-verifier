package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.repository.GlobalRepository;
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

  private static List<String> findPreviousBuilds(String currentBuild) throws IOException {
    List<String> resultsOnInPluginRepository = GlobalRepository.loadAvailableCheckResultsList();

    String firstBuild = System.getProperty("firstBuild");
    if (firstBuild != null) {
      int idx = resultsOnInPluginRepository.indexOf(firstBuild);
      if (idx != -1) {
        resultsOnInPluginRepository = resultsOnInPluginRepository.subList(idx, resultsOnInPluginRepository.size());
      }
    }

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

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (freeArgs.isEmpty()) {
      throw Util.fail("You have to specify IDE to check. For example: \"java -jar verifier.jar new-problems report-133.439.xml\"");
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

    problems.removeAll(firstBuildResult.getAllProblems());

    int newProblemsCount = problems.size();

    for (int i = 1; i < previousCheckedBuild.size(); i++) {
      String prevBuild = previousCheckedBuild.get(i);
      ResultsElement prevBuildResult = ProblemUtils.loadProblems(DownloadUtils.getCheckResult(prevBuild));

      for (Problem problem : prevBuildResult.getAllProblems()) {
        if (problems.remove(problem)) {
          buildToProblems.put(prevBuild, problem);
        }
      }
    }

    String currentBuildName = "#" + checkResult.getIde();

    buildToProblems.putAll(currentBuildName, problems);

    List<Pair<String, String>> tcMessages = new ArrayList<Pair<String, String>>();

    for (String prevBuild : Iterables.concat(previousCheckedBuild.subList(1, previousCheckedBuild.size()), Collections.singleton(currentBuildName))) {
      Collection<Problem> problemsInBuild = buildToProblems.get(prevBuild);
      if (!problemsInBuild.isEmpty()) {
        System.out.printf("\nIn %s found %d new problems:\n", prevBuild,problemsInBuild.size());

        for (Problem problem : ProblemUtils.sort(problemsInBuild)) {
          CharSequence problemDescription = MessageUtils.cutCommonPackages(problem.getDescription());
          Collection<UpdateInfo> affectedUpdates = problemsToUpdates.get(problem);

          System.out.print("    ");
          System.out.println(problemDescription);
          System.out.println("        in " + Joiner.on(", ").join(affectedUpdates));

          tcMessages.add(Pair.create("since " + prevBuild + "  " + problemDescription + " (in " + Joiner.on(", ").join(affectedUpdates) + ')', ProblemUtils.hash(problem)));
        }
      }
    }

    TeamCityLog tc = TeamCityLog.getInstance(commandLine);

    for (int i = tcMessages.size() - 1; i >= 0; i--) {
      tc.buildProblem(tcMessages.get(i).first, tcMessages.get(i).second);
    }

    tc.buildStatusSuccess(String.format("Done, %d new problems found between %s and %s",
                                        newProblemsCount,
                                        previousCheckedBuild.get(0),
                                        previousCheckedBuild.get(previousCheckedBuild.size() - 1)));

    return 0;
  }
}
