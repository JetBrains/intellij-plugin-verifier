package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.GlobalResultsRepository;
import com.jetbrains.pluginverifier.results.ResultsElement;
import com.jetbrains.pluginverifier.results.ResultsRepository;
import com.jetbrains.pluginverifier.utils.*;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityLog;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityUtil;
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

  /**
   * @return list of IDEA builds for which check is already performed and
   * its trunk number equals to the given trunk number (e.g. 141 and 141)
   * and the build number is LESS than the given build number.
   * NOTE: in ascending order, i.e. 141.01, 141.05, 141.264...
   */
  @NotNull
  public static List<String> findPreviousBuilds(@NotNull String currentBuild,
                                                @NotNull ResultsRepository resultsRepository) throws IOException {
    //for now method and repository support only "IU-X.Y" build's form

    List<String> resultsInPluginRepository = resultsRepository.getAvailableReportsList();

    String firstBuild = System.getProperty("firstBuild");
    if (firstBuild != null) {
      //filter builds so that only the firstBuild and older are kept
      int idx = resultsInPluginRepository.indexOf(firstBuild);
      if (idx != -1) {
        resultsInPluginRepository = resultsInPluginRepository.subList(idx, resultsInPluginRepository.size());
      }
    }

    Pair<String, Integer> parsedCurrentBuild = parseBuildNumber(currentBuild);

    TreeMap<Integer, String> buildMap = new TreeMap<Integer, String>();

    for (String build : resultsInPluginRepository) {
      Pair<String, Integer> pair = parseBuildNumber(build);

      //NOTE: compares only IDEAs of the same release! that is 141.* between each others
      if (parsedCurrentBuild.first.equals(pair.first) && parsedCurrentBuild.second > pair.second) {
        buildMap.put(pair.second, build);
      }
    }

    return new ArrayList<String>(buildMap.values());
  }

  /**
   * e.g. IU-141.1532 -> < IU-141, 1532>
   */
  @NotNull
  private static Pair<String, Integer> parseBuildNumber(String buildNumber) {
    int idx = buildNumber.indexOf('.');

    return Pair.create(buildNumber.substring(0, idx), Integer.parseInt(buildNumber.substring(idx + 1)));
  }

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (freeArgs.isEmpty()) {
      throw FailUtil.fail("You have to specify IDE to check. For example: \"java -jar verifier.jar new-problems report-133.439.xml\"");
    }

    TeamCityUtil.ReportGrouping reportGrouping = TeamCityUtil.ReportGrouping.parseGrouping(commandLine);

    File reportToCheck = new File(freeArgs.get(0));
    if (!reportToCheck.isFile()) {
      throw FailUtil.fail("Report not found: " + reportToCheck);
    }

    ResultsElement currentCheckResult = ProblemUtils.loadProblems(reportToCheck);

    ResultsRepository resultsRepository = new GlobalResultsRepository();

    List<String> previousCheckedBuilds = findPreviousBuilds(currentCheckResult.getIde(), resultsRepository);

    if (previousCheckedBuilds.isEmpty()) {
      System.err.println("Plugin repository does not contain check result to compare.");
      return 0;
    }


    //---------------------------------------------------

    Multimap<Problem, UpdateInfo> currentProblemsMap = ProblemUtils.rearrangeProblemsMap(currentCheckResult.asMap());


    //Problems of this check
    Set<Problem> currProblems = new HashSet<Problem>(currentProblemsMap.keySet());

    //leave only NEW' problems of this check compared to the EARLIEST check
    ResultsElement smallestCheckResult = ProblemUtils.loadProblems(resultsRepository.getReportFile(previousCheckedBuilds.get(0)));

    //remove old API problems
    currProblems.removeAll(smallestCheckResult.getProblems());

    //Map: <Build Number -> List[Problem for which this problem occurred first]>
    Multimap<String, Problem> firstOccurrenceBuildToProblems = ArrayListMultimap.create();

    for (int i = 1; i < previousCheckedBuilds.size(); i++) {
      String prevBuild = previousCheckedBuilds.get(i);

      //check result in ascending order of builds
      ResultsElement prevBuildResult = ProblemUtils.loadProblems(resultsRepository.getReportFile(prevBuild));

      for (Problem problem : prevBuildResult.getProblems()) {
        if (currProblems.remove(problem)) {
          firstOccurrenceBuildToProblems.put(prevBuild, problem);
        }
      }
    }

    final String currentBuildName = currentCheckResult.getIde();

    //map of UNRESOLVED problems: <IDEA-build -> ALL the problems of this build (in which these problems were met first)>
    firstOccurrenceBuildToProblems.putAll(currentBuildName, currProblems);

    //---------------------------------------------------


    //ALL the checked builds (excluding the EARLIEST one)
    Iterable<String> allBuilds = Iterables.concat(previousCheckedBuilds.subList(1, previousCheckedBuilds.size()), Collections.singleton(currentBuildName));

    TeamCityLog tc = TeamCityLog.getInstance(commandLine);

    for (String prevBuild : allBuilds) {
      Collection<Problem> problemsInBuild = firstOccurrenceBuildToProblems.get(prevBuild);

      TeamCityLog.TestSuite suite = tc.testSuiteStarted("since " + prevBuild);

      if (!problemsInBuild.isEmpty()) {
        System.out.printf("\nIn %s found %d new problems:\n", prevBuild, problemsInBuild.size());

        ArrayListMultimap<Problem, UpdateInfo> prevBuildProblems = ArrayListMultimap.create();
        for (Problem problem : problemsInBuild) {
          Collection<UpdateInfo> affectedUpdates = ProblemUtils.sortUpdates(currentProblemsMap.get(problem));
          prevBuildProblems.putAll(problem, affectedUpdates);

          CharSequence problemDescription = MessageUtils.cutCommonPackages(problem.getDescription());

          System.out.print("    ");
          System.out.println(problemDescription);
          System.out.println("        in " + Joiner.on(", ").join(affectedUpdates));

        }

        TeamCityUtil.printReport(tc, prevBuildProblems, reportGrouping);
      }

      suite.close();

    }


    //number of NEW problems (excluding the EARLIEST check)
    final int newProblemsCount = currProblems.size();

    final String text = String.format("Done, %d new %s found between %s and %s. Current build is %s",
        newProblemsCount,
        StringUtil.pluralize("problem", newProblemsCount),
        previousCheckedBuilds.get(0),
        previousCheckedBuilds.get(previousCheckedBuilds.size() - 1),
        currentCheckResult.getIde());

    if (newProblemsCount > 0) {
      tc.buildStatusFailure(text);
    } else {
      tc.buildStatusSuccess(text);
    }

    return 0;
  }
}
