package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.intellij.structure.utils.ProductUpdateBuild;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.GlobalResultsRepository;
import com.jetbrains.pluginverifier.results.ResultsElement;
import com.jetbrains.pluginverifier.results.ResultsRepository;
import com.jetbrains.pluginverifier.results.VerifierServiceRepository;
import com.jetbrains.pluginverifier.utils.FailUtil;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import com.jetbrains.pluginverifier.utils.StringUtil;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityLog;
import com.jetbrains.pluginverifier.utils.teamcity.TeamCityUtil;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Prints new problems of current plugins verifications compared to all
 * the previous verifications of the same trunk builds (i.e. all IDEs 143.*)
 *
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
  public static List<String> findPreviousBuilds(@NotNull String currentCheckBuild,
                                                @NotNull ResultsRepository resultsRepository) throws IOException {

    List<String> resultsInPluginRepository = resultsRepository.getAvailableReportsList();
    Collections.sort(resultsInPluginRepository, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        ProductUpdateBuild build1 = new ProductUpdateBuild(o1);
        ProductUpdateBuild build2 = new ProductUpdateBuild(o2);
        return ProductUpdateBuild.VERSION_COMPARATOR.compare(build1, build2);
      }
    });

    String firstBuildProp = System.getProperty("firstBuild");
    if (firstBuildProp != null) {
      ProductUpdateBuild firstBuild = new ProductUpdateBuild(firstBuildProp);
      int idx = -1;
      for (int i = 0; i < resultsInPluginRepository.size(); i++) {
        String build = resultsInPluginRepository.get(i);
        if (ProductUpdateBuild.VERSION_COMPARATOR.compare(firstBuild, new ProductUpdateBuild(build)) == 0) {
          idx = i;
          break;
        }
      }
      if (idx != -1) {
        resultsInPluginRepository = resultsInPluginRepository.subList(idx, resultsInPluginRepository.size());
      }
    }

    ProductUpdateBuild currentBuild = new ProductUpdateBuild(currentCheckBuild);

    List<String> result = new ArrayList<String>();

    for (String build : resultsInPluginRepository) {
      ProductUpdateBuild updateBuild = new ProductUpdateBuild(build);

      //NOTE: compares only IDEAs of the same branch! that is 141.* between each others
      if (updateBuild.getBranch() == currentBuild.getBranch() && ProductUpdateBuild.VERSION_COMPARATOR.compare(updateBuild, currentBuild) < 0) {
        result.add(build);
      }
    }

    return result;
  }

  private static void printTcProblems(@NotNull Multimap<Problem, UpdateInfo> currentProblemsMap,
                                      @NotNull Multimap<String, Problem> firstOccurrence,
                                      @NotNull Iterable<String> allBuilds,
                                      @NotNull TeamCityUtil.ReportGrouping reportGrouping,
                                      @NotNull TeamCityLog tc) {
    for (String prevBuild : allBuilds) {
      Collection<Problem> problemsInBuild = firstOccurrence.get(prevBuild);

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
  }

  @NotNull
  private static ResultsRepository getResultsRepository(@NotNull CommandLine commandLine) {
    ResultsRepository resultsRepository;
    String repoUrl = commandLine.getOptionValue("repo");
    if (repoUrl == null) {
      System.out.println("Results repository set to global repository");
      resultsRepository = new GlobalResultsRepository();
    } else {
      System.out.println("Results repository set to " + repoUrl);
      resultsRepository = new VerifierServiceRepository(repoUrl);
    }
    return resultsRepository;
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

    ResultsRepository resultsRepository = getResultsRepository(commandLine);

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

    printTcProblems(currentProblemsMap, firstOccurrenceBuildToProblems, allBuilds, reportGrouping, tc);


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
