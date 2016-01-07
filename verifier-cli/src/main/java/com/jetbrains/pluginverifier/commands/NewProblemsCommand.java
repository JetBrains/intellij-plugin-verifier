package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.intellij.structure.domain.IdeVersion;
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
 * Prints new problems of current plugins verification compared to all
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
        IdeVersion build1 = new IdeVersion(o1);
        IdeVersion build2 = new IdeVersion(o2);
        return IdeVersion.VERSION_COMPARATOR.compare(build1, build2);
      }
    });

    String firstBuildProp = System.getProperty("firstBuild");
    if (firstBuildProp != null) {
      IdeVersion firstBuild = new IdeVersion(firstBuildProp);
      int idx = -1;
      for (int i = 0; i < resultsInPluginRepository.size(); i++) {
        String build = resultsInPluginRepository.get(i);
        if (IdeVersion.VERSION_COMPARATOR.compare(firstBuild, new IdeVersion(build)) == 0) {
          idx = i;
          break;
        }
      }
      if (idx != -1) {
        resultsInPluginRepository = resultsInPluginRepository.subList(idx, resultsInPluginRepository.size());
      }
    }

    IdeVersion currentBuild = new IdeVersion(currentCheckBuild);

    List<String> result = new ArrayList<String>();

    for (String build : resultsInPluginRepository) {
      IdeVersion updateBuild = new IdeVersion(build);

      //NOTE: compares only IDEAs of the same branch! that is 141.* between each others
      if (updateBuild.getBranch() == currentBuild.getBranch() && IdeVersion.VERSION_COMPARATOR.compare(updateBuild, currentBuild) < 0) {
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

        Multimap<Problem, UpdateInfo> prevBuildProblems = ArrayListMultimap.create();
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

    ResultsElement checkResult = ProblemUtils.loadProblems(reportToCheck);

    final Map<UpdateInfo, Collection<Problem>> updateToProblems = checkResult.asMap();
    final String ideBuild = checkResult.getIde();


    //--------------Load previous checks-------------------

    ResultsRepository resultsRepository = getResultsRepository(commandLine);

    List<String> checkedBuilds;
    try {
      checkedBuilds = findPreviousBuilds(ideBuild, resultsRepository);
    } catch (IOException e) {
      throw FailUtil.fail("Couldn't get check results list from the server " + resultsRepository.getRepositoryUrl(), e);
    }

    List<ResultsElement> checks = new ArrayList<ResultsElement>();
    try {
      for (String build : checkedBuilds) {
        checks.add(ProblemUtils.loadProblems(resultsRepository.getReportFile(build)));
      }
    } catch (IOException e) {
      throw FailUtil.fail("Couldn't get check from the server " + resultsRepository.getRepositoryUrl(), e);
    }

    if (checks.isEmpty()) {
      System.err.println("Plugin repository does not contain check result to compare.");
      return 0;
    }

    //--------------Drop out old API problems------------------

    dropUnrelatedProblems(updateToProblems, checks);

    Multimap<Problem, UpdateInfo> currentProblems = ProblemUtils.flipProblemsMap(updateToProblems);

    //Problems of this check
    Set<Problem> currProblems = new HashSet<Problem>(currentProblems.keySet());

    //remove old API problems
    currProblems.removeAll(checks.get(0).getProblems());

    //Map: <Build Number -> List[Problem for which this problem occurred first]>
    Multimap<String, Problem> firstOccurrence = ArrayListMultimap.create();

    for (int i = 1; i < checkedBuilds.size(); i++) {
      String prevBuild = checkedBuilds.get(i);

      for (Problem problem : checks.get(i).getProblems()) {
        if (currProblems.remove(problem)) {
          firstOccurrence.put(prevBuild, problem);
        }
      }
    }

    //map of unresolved problems: <IDEA-build -> All the problems of this build (in which these problems were met first)>
    firstOccurrence.putAll(ideBuild, currProblems);

    //---------------Print new API problems-------------------

    //ALL the checked builds (excluding the EARLIEST one)
    Iterable<String> allBuilds = Iterables.concat(checkedBuilds.subList(1, checkedBuilds.size()), Collections.singleton(ideBuild));

    TeamCityLog tc = TeamCityLog.getInstance(commandLine);

    printTcProblems(currentProblems, firstOccurrence, allBuilds, reportGrouping, tc);


    //number of problems appeared in the trunk (e.g. in 144.* builds)
    final int totalTrunkProblems = firstOccurrence.values().size();

    //number of the newest problems (of the last IDEA build)
    final int newProblemsCount = currProblems.size();

    final String text = String.format("Done, %d new %s in %s; %d problems between %s and %s",
        newProblemsCount,
        StringUtil.pluralize("problem", newProblemsCount),
        ideBuild,
        totalTrunkProblems,
        checkedBuilds.get(0),
        checkedBuilds.get(checkedBuilds.size() - 1)
    );

    if (newProblemsCount > 0) {
      tc.buildStatusFailure(text);
    } else {
      tc.buildStatusSuccess(text);
    }

    return 0;
  }

  /**
   * Drops out all the problems of plugins which were detected in
   * the previous corresponding plugin-builds
   * (because it's probably not the API breakage, but plugin severe incompatibility):
   * e.g. plugin requires JDK 1.7 but check was performed against JDK 1.6 and some
   * referenced classes (java.nio.file.Files etc.) were not found - so this is not a problem of the IntelliJ API
   *
   * @param updateToProblems current check result
   * @param checks           all the previous checks
   */
  private void dropUnrelatedProblems(@NotNull Map<UpdateInfo, Collection<Problem>> updateToProblems,
                                     @NotNull List<ResultsElement> checks) {
    Multimap<String, UpdateInfo> idToUpdates = fillPluginIdToUpdates(updateToProblems, checks);

    for (Map.Entry<UpdateInfo, Collection<Problem>> entry : updateToProblems.entrySet()) {
      UpdateInfo update = entry.getKey();
      Collection<Problem> problems = entry.getValue();

      Collection<Problem> prevBuildProblems = getPreviousBuildProblems(update, idToUpdates, checks);

      problems.removeAll(prevBuildProblems);
    }
  }

  @NotNull
  private Collection<Problem> getPreviousBuildProblems(@NotNull UpdateInfo curUpdate,
                                                       @NotNull Multimap<String, UpdateInfo> idToUpdates,
                                                       @NotNull List<ResultsElement> checks) {
    List<UpdateInfo> allUpdates = new ArrayList<UpdateInfo>(idToUpdates.get(curUpdate.getPluginId()));
    Collections.sort(allUpdates, UpdateInfo.UPDATE_NUMBER_COMPARATOR);

    List<Problem> allProblems = new ArrayList<Problem>();

    for (UpdateInfo update : Lists.reverse(allUpdates)) {
      if (UpdateInfo.UPDATE_NUMBER_COMPARATOR.compare(update, curUpdate) < 0) {

        for (ResultsElement check : checks) {
          Collection<Problem> problems = check.asMap().get(update);
          if (problems != null) {
            allProblems.addAll(problems);
          }
        }
      }
    }

    return allProblems;
  }

  @NotNull
  private Multimap<String, UpdateInfo> fillPluginIdToUpdates(@NotNull Map<UpdateInfo, Collection<Problem>> updateToProblems,
                                                             @NotNull List<ResultsElement> checks) {
    Multimap<String, UpdateInfo> idToUpdates = ArrayListMultimap.create();

    for (UpdateInfo updateInfo : updateToProblems.keySet()) {
      idToUpdates.put(updateInfo.getPluginId(), updateInfo);
    }

    for (ResultsElement check : checks) {
      for (UpdateInfo updateInfo : check.asMap().keySet()) {
        idToUpdates.put(updateInfo.getPluginId(), updateInfo);
      }
    }
    return idToUpdates;
  }

}
