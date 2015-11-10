package com.jetbrains.pluginverifier.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.jetbrains.pluginverifier.problems.FailUtil;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.jetbrains.pluginverifier.utils.StringUtil.pluralize;

/**
 * @author Sergey Evdokimov
 */
public class TeamCityUtil {

  private static void notGrouped(@NotNull TeamCityLog log, @NotNull Multimap<Problem, UpdateInfo> problems) {
    List<Problem> sortedProblems = ProblemUtils.sortProblems(problems.keySet());

    for (Problem problem : sortedProblems) {
      List<UpdateInfo> updates = new ArrayList<UpdateInfo>(problems.get(problem));

      ProblemUtils.sortUpdates(updates);

      log.buildProblem(MessageUtils.cutCommonPackages(problem.getDescription()) + " (in " + Joiner.on(", ").join(updates) + ')');
    }
  }

  public static void printReport(@NotNull TeamCityLog log,
                                 @NotNull Multimap<Problem, UpdateInfo> problems,
                                 @NotNull ReportGrouping reportGrouping) {
    if (log == TeamCityLog.NULL_LOG) return;
    if (problems.isEmpty()) return;

    Multimap<UpdateInfo, Problem> inverted = invertMultimap(problems);

    switch (reportGrouping) {
      case NONE:
        notGrouped(log, problems);
        break;
      case PLUGIN:
        groupByPlugin(log, inverted.asMap());
        break;
      case PROBLEM_TYPE:
        groupByType(log, inverted.asMap());
        break;
    }
  }

  @NotNull
  private static Multimap<UpdateInfo, Problem> invertMultimap(@NotNull Multimap<Problem, UpdateInfo> problem2Updates) {
    ArrayListMultimap<UpdateInfo, Problem> result = ArrayListMultimap.create();
    Multimaps.invertFrom(problem2Updates, result);
    return result;
  }


  public static void groupByType(@NotNull TeamCityLog log, @NotNull Map<UpdateInfo, Collection<Problem>> map) {
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
  private static ArrayListMultimap<String, UpdateInfo> fillIdToUpdates(@NotNull Map<UpdateInfo, Collection<Problem>> map) {
    ArrayListMultimap<String, UpdateInfo> idToUpdates = ArrayListMultimap.create();
    for (UpdateInfo updateInfo : map.keySet()) {
      String pluginId = updateInfo.getPluginId() != null ? updateInfo.getPluginId() : "#" + updateInfo.getUpdateId();
      idToUpdates.put(pluginId, updateInfo);
    }
    return idToUpdates;
  }

  @NotNull
  private static String cutCommonPrefix(@NotNull List<Problem> problems) {
    return Joiner.on(" ").join(commonPrefix(problems));
  }

  @NotNull
  private static String[] commonPrefix(@NotNull List<Problem> problems) {
    if (problems.isEmpty()) return new String[0];
    String[] prefix = problems.get(0).getDescription().split(" ");
    for (Problem problem : problems) {
      prefix = commonPrefix(prefix, problem.getDescription().split(" "));
    }
    return prefix;
  }

  @NotNull
  private static String[] commonPrefix(@NotNull String a[], @NotNull String b[]) {
    return Arrays.copyOf(a, commonPrefixLength(a, b));
  }

  public static int commonPrefixLength(@NotNull String a[], @NotNull String b[]) {
    int i;
    int minLength = Math.min(a.length, b.length);
    for (i = 0; i < minLength; i++) {
      if (!StringUtil.equals(a[i], b[i])) {
        break;
      }
    }
    return i;
  }

  public static void groupByPlugin(@NotNull TeamCityLog log, @NotNull Map<UpdateInfo, Collection<Problem>> map) {

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


  public enum ReportGrouping {
    PLUGIN,
    PROBLEM_TYPE,
    NONE;

    @NotNull
    public static ReportGrouping parseGrouping(@NotNull CommandLine commandLine) {
      ReportGrouping grouping = ReportGrouping.NONE;
      String groupValue = commandLine.getOptionValue("g");
      if (groupValue != null) {
        if ("plugin".equals(groupValue)) {
          grouping = ReportGrouping.PLUGIN;
        } else if ("type".equals(groupValue)) {
          grouping = ReportGrouping.PROBLEM_TYPE;
        } else {
          throw FailUtil.fail("Grouping argument should be one of 'plugin' or 'type' or not set at all.");
        }
      }
      return grouping;
    }
  }
}
