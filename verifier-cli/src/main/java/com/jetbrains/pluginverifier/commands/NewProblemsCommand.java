package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.problems.FailUtil;
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

  /**
   * @return list of IDEA builds for which check is already performed and
   * its main-part equals to the given main-part (141 and 141)
   * and the build number is LESS than the given build number.
   * NOTE: in ascending order, i.e. 141.01, 141.05, 141.264...
   */
  private static List<String> findPreviousBuilds(String currentBuild) throws IOException {
    List<String> resultsOnInPluginRepository = GlobalRepository.loadAvailableCheckResultsList();

    String firstBuild = System.getProperty("firstBuild");
    if (firstBuild != null) {
      //filter builds so that only the firstBuild and older are kept
      int idx = resultsOnInPluginRepository.indexOf(firstBuild);
      if (idx != -1) {
        resultsOnInPluginRepository = resultsOnInPluginRepository.subList(idx, resultsOnInPluginRepository.size());
      }
    }

    Pair<String, Integer> parsedCurrentBuild = parseBuildNumber(currentBuild);

    TreeMap<Integer, String> buildMap = new TreeMap<Integer, String>();

    for (String build : resultsOnInPluginRepository) {
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
  private static Pair<String, Integer> parseBuildNumber(String buildNumber) {
    int idx = buildNumber.lastIndexOf('.');

    return Pair.create(buildNumber.substring(0, idx), Integer.parseInt(buildNumber.substring(idx + 1)));
  }

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    if (freeArgs.isEmpty()) {
      throw FailUtil.fail("You have to specify IDE to check. For example: \"java -jar verifier.jar new-problems report-133.439.xml\"");
    }

    File reportToCheck = new File(freeArgs.get(0));
    if (!reportToCheck.isFile()) {
      throw FailUtil.fail("Report not found: " + reportToCheck);
    }

    ResultsElement currentCheckResult = ProblemUtils.loadProblems(reportToCheck);

    List<String> previousCheckedBuilds = findPreviousBuilds(currentCheckResult.getIde());

    if (previousCheckedBuilds.isEmpty()) {
      System.out.println("Plugin repository does not contain check result to compare.");
      return 0;
    }


    //---------------------------------------------------

    Multimap<Problem, UpdateInfo> currentProblemsToUpdates = ProblemUtils.rearrangeProblemsMap(currentCheckResult.asMap());


    //Problems of this check
    Set<Problem> currProblems = new HashSet<Problem>(currentProblemsToUpdates.keySet());

    //leave only NEW' problems of this check compared to the EARLIEST check
    ResultsElement smallestCheckResult = ProblemUtils.loadProblems(DownloadUtils.getCheckResultFile(previousCheckedBuilds.get(0)));
    currProblems.removeAll(smallestCheckResult.getProblems());

    //Map: <Build Number -> List[Problem for which this problem occurred first]>
    Multimap<String, Problem> firstOccurrenceBuildToProblems = ArrayListMultimap.create();

    for (int i = 1; i < previousCheckedBuilds.size(); i++) {
      String prevBuild = previousCheckedBuilds.get(i);

      //check result in ascending order of builds
      ResultsElement prevBuildResult = ProblemUtils.loadProblems(DownloadUtils.getCheckResultFile(prevBuild));

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


    List<Pair<String, String>> tcMessages = new ArrayList<Pair<String, String>>();

    //TODO: somehow rewrite this

    //ALL the builds (excluding the EARLIEST one) AND (including this one)
    Iterable<String> allBuilds = Iterables.concat(previousCheckedBuilds.subList(1, previousCheckedBuilds.size()), Collections.singleton(currentBuildName));

    for (String prevBuild : allBuilds) {
      Collection<Problem> problemsInBuild = firstOccurrenceBuildToProblems.get(prevBuild);

      //For the IDEA-build list of yet UNRESOLVED problems
      if (!problemsInBuild.isEmpty()) {
        System.out.printf("\nIn %s found %d new problems:\n", prevBuild, problemsInBuild.size());

        //in sorted by problem-description order
        for (Problem problem : ProblemUtils.sortProblems(problemsInBuild)) {
          CharSequence problemDescription = MessageUtils.cutCommonPackages(problem.getDescription());
          Collection<UpdateInfo> affectedUpdates = ProblemUtils.sortUpdates(new ArrayList<UpdateInfo>(currentProblemsToUpdates.get(problem)));

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

    //number of NEW' problems (compared to the EARLIEST check)
    final int newProblemsCount = currProblems.size();

    tc.buildStatusSuccess(String.format("Done, %d new problems found between %s and %s. Current build is %s",
        newProblemsCount,
        previousCheckedBuilds.get(0),
        previousCheckedBuilds.get(previousCheckedBuilds.size() - 1),
        currentCheckResult.getIde())
    );

    return 0;
  }
}
