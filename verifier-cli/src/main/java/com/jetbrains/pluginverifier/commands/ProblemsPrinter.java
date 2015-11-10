package com.jetbrains.pluginverifier.commands;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.jetbrains.pluginverifier.VerifierCommand;
import com.jetbrains.pluginverifier.problems.FailUtil;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import com.jetbrains.pluginverifier.utils.*;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;

import static com.jetbrains.pluginverifier.utils.StringUtil.pluralize;

/**
 * @author Sergey Patrikeev
 */
public class ProblemsPrinter extends VerifierCommand {

  public ProblemsPrinter() {
    super("print-problems");
  }

  @NotNull
  private static ArrayListMultimap<String, UpdateInfo> fillIdToUpdates(Map<UpdateInfo, Collection<Problem>> map) {
    ArrayListMultimap<String, UpdateInfo> idToUpdates = ArrayListMultimap.create();
    for (UpdateInfo updateInfo : map.keySet()) {
      String pluginId = updateInfo.getPluginId() != null ? updateInfo.getPluginId() : "#" + updateInfo.getUpdateId();
      idToUpdates.put(pluginId, updateInfo);
    }
    return idToUpdates;
  }

  @Override
  public int execute(@NotNull CommandLine commandLine, @NotNull List<String> freeArgs) throws Exception {
    TeamCityLog log = TeamCityLog.getInstance(commandLine);

    String file = commandLine.getOptionValue("printFile");

    if (file == null) {
      throw FailUtil.fail("You have to specify report-file to be printed");
    }

    File reportToCheck = new File(file);
    if (!reportToCheck.isFile()) {
      throw FailUtil.fail("Report not found: " + reportToCheck);
    }

    TeamCityUtil.ReportGrouping grouping = TeamCityUtil.ReportGrouping.parseGrouping(commandLine);

    ResultsElement currentCheckResult = ProblemUtils.loadProblems(reportToCheck);
    Map<UpdateInfo, Collection<Problem>> map = currentCheckResult.asMap();

    switch (grouping) {
      case NONE:
        break;
      case PLUGIN:
        groupByPlugin(log, map);
        break;
      case PROBLEM_TYPE:
        groupByType(log, map);
        break;
    }

    final int totalProblems = new HashSet<Problem>(currentCheckResult.getProblems()).size(); //number of unique problems

    String description = "IDE " + currentCheckResult.getIde() + " has " + totalProblems + " " + pluralize("problem", totalProblems);
    log.buildProblem(description);
    log.buildStatus(description);

    return 0;
  }

  private void groupByType(@NotNull TeamCityLog log, @NotNull Map<UpdateInfo, Collection<Problem>> map) {
    Multimap<Problem, UpdateInfo> problem2Updates = ProblemUtils.rearrangeProblemsMap(map);

    ArrayListMultimap<String, Problem> problemType2Problem = ArrayListMultimap.create();
    for (Problem problem : problem2Updates.keySet()) {
      problemType2Problem.put(problem.getClass().getCanonicalName(), problem);
    }

    for (String problemType : problemType2Problem.keySet()) {
      List<Problem> problems = ProblemUtils.sortProblems(problemType2Problem.get(problemType));

      if (problems.isEmpty()) continue;

      String commonPrefix = cutCommonPrefix(problems);
      TeamCityLog.TestSuite problemTypeSuite = log.testSuiteStarted(commonPrefix);

      for (Problem problem : problems) {
        String description = StringUtil.trimStart(problem.getDescription(), commonPrefix);
        Collection<UpdateInfo> updateInfos = problem2Updates.get(problem);

        TeamCityLog.TestSuite problemSuite = log.testSuiteStarted(description);

        for (UpdateInfo updateInfo : updateInfos) {
          String plugin = updateInfo.toString().replace('.', ',');
          TeamCityLog.Test test = log.testStarted(plugin);
          log.testFailed(plugin, "Plugin " + updateInfo + " has the following problem", problem.getDescription());
          test.close();
        }

        problemSuite.close();
      }

      problemTypeSuite.close();
    }

  }

  @NotNull
  private String cutCommonPrefix(@NotNull List<Problem> problems) {
    return Joiner.on(" ").join(commonPrefix(problems));
  }

  @NotNull
  private String[] commonPrefix(@NotNull List<Problem> problems) {
    if (problems.isEmpty()) return new String[0];
    String[] prefix = problems.get(0).getDescription().split(" ");
    for (Problem problem : problems) {
      prefix = commonPrefix(prefix, problem.getDescription().split(" "));
    }
    return prefix;
  }

  @NotNull
  private String[] commonPrefix(@NotNull String a[], @NotNull String b[]) {
    return Arrays.copyOf(a, commonPrefixLength(a, b));
  }

  public int commonPrefixLength(@NotNull String a[], @NotNull String b[]) {
    int i;
    int minLength = Math.min(a.length, b.length);
    for (i = 0; i < minLength; i++) {
      if (!StringUtil.equals(a[i], b[i])) {
        break;
      }
    }
    return i;
  }

  private void groupByPlugin(TeamCityLog log, Map<UpdateInfo, Collection<Problem>> map) {
    ArrayListMultimap<String, UpdateInfo> idToUpdates = fillIdToUpdates(map);

    for (String pluginId : idToUpdates.keySet()) {
      List<UpdateInfo> updateInfos = idToUpdates.get(pluginId);
      ProblemUtils.sortUpdates(updateInfos);

      TeamCityLog.TestSuite pluginSuite = log.testSuiteStarted(pluginId);

      for (UpdateInfo updateInfo : updateInfos) {

        List<Problem> problems = ProblemUtils.sortProblems(map.get(updateInfo));

        if (!problems.isEmpty()) {
          String version = updateInfo.getVersion() != null ? updateInfo.getVersion() : "#" + updateInfo.getUpdateId();

          StringBuilder builder = new StringBuilder();

          for (Problem problem : problems) {
            builder.append("#").append(problem.getDescription()).append("\n");
          }

          String testName = version.replace('.', ',');
          TeamCityLog.Test test = log.testStarted(testName);
          log.testStdErr(testName, builder.toString());
          log.testFailed(testName, updateInfo + " has " + problems.size() + " " + pluralize("problem", problems.size()), "");
          test.close();

        }
      }
      pluginSuite.close();
    }
  }
}
