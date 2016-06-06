package com.jetbrains.pluginverifier.utils.teamcity;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.jetbrains.pluginverifier.commands.CheckIdeCommand;
import com.jetbrains.pluginverifier.format.UpdateInfo;
import com.jetbrains.pluginverifier.location.ProblemLocation;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.results.ProblemSet;
import com.jetbrains.pluginverifier.utils.FailUtil;
import com.jetbrains.pluginverifier.utils.MessageUtils;
import com.jetbrains.pluginverifier.utils.ProblemUtils;
import com.jetbrains.pluginverifier.utils.StringUtil;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static com.jetbrains.pluginverifier.utils.StringUtil.pluralize;

/**
 * @author Sergey Evdokimov
 */
public class TeamCityUtil {

  private static final String REPOSITORY_PLUGIN_ID_BASE = "https://plugins.jetbrains.com/plugin/index?xmlId=";

  private static void notGrouped(@NotNull TeamCityLog log, @NotNull Multimap<Problem, UpdateInfo> problems) {
    List<Problem> sortedProblems = ProblemUtils.sortProblems(problems.keySet());

    for (Problem problem : sortedProblems) {
      List<UpdateInfo> updates = ProblemUtils.sortUpdatesWithDescendingVersionsOrder(problems.get(problem));

      log.buildProblem(MessageUtils.cutCommonPackages(problem.getDescription()) + " (in " + Joiner.on(", ").join(updates) + ')');
    }
  }

  public static void printReportWithLocations(@NotNull TeamCityLog log,
                                              @NotNull Map<UpdateInfo, ProblemSet> results) {
    if (log == TeamCityLog.NULL_LOG) return;
    if (results.isEmpty()) return;

    groupByPlugin(log, results);
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
        groupByPlugin(log, fillWithEmptyLocations(inverted.asMap()));
        break;
      case PROBLEM_TYPE:
        groupByType(log, inverted.asMap());
        break;
    }
  }

  @NotNull
  public static Map<UpdateInfo, ProblemSet> fillWithEmptyLocations(@NotNull Map<UpdateInfo, Collection<Problem>> map) {
    Map<UpdateInfo, ProblemSet> result = new HashMap<UpdateInfo, ProblemSet>();

    final Set<ProblemLocation> emptySet = Collections.emptySet();

    for (Map.Entry<UpdateInfo, Collection<Problem>> entry : map.entrySet()) {
      Map<Problem, Set<ProblemLocation>> problemMap = new HashMap<Problem, Set<ProblemLocation>>();

      for (Problem problem : entry.getValue()) {
        problemMap.put(problem, emptySet);
      }
      result.put(entry.getKey(), new ProblemSet(problemMap));

    }
    return result;
  }

  @NotNull
  private static Multimap<UpdateInfo, Problem> invertMultimap(@NotNull Multimap<Problem, UpdateInfo> problem2Updates) {
    ArrayListMultimap<UpdateInfo, Problem> result = ArrayListMultimap.create();
    Multimaps.invertFrom(problem2Updates, result);
    return result;
  }

  @NotNull
  private static String getPluginUrl(@NotNull UpdateInfo updateInfo) {
    return REPOSITORY_PLUGIN_ID_BASE + (updateInfo.getPluginId() != null ? updateInfo.getPluginId() : updateInfo.getPluginName());
  }

  public static void groupByType(@NotNull TeamCityLog log, @NotNull Map<UpdateInfo, Collection<Problem>> map) {
    Multimap<Problem, UpdateInfo> problem2Updates = ProblemUtils.flipProblemsMap(map);

    Multimap<String, Problem> problemType2Problem = ArrayListMultimap.create();
    for (Problem problem : problem2Updates.keySet()) {
      problemType2Problem.put(problem.getClass().getCanonicalName(), problem);
    }

    for (String problemType : problemType2Problem.keySet()) {
      List<Problem> problems = ProblemUtils.sortProblems(problemType2Problem.get(problemType));

      if (problems.isEmpty()) continue;

      String prefix = convertNameToPrefix(problemType);
      TeamCityLog.TestSuite problemTypeSuite = log.testSuiteStarted("(" + prefix + ")");

      for (Problem problem : problems) {
        String description = StringUtil.trimStart(problem.getDescription(), prefix).trim();
        Collection<UpdateInfo> updateInfos = problem2Updates.get(problem);

        TeamCityLog.TestSuite problemSuite = log.testSuiteStarted("[" + description + "]");

        for (UpdateInfo updateInfo : updateInfos) {
          String plugin = "(" + updateInfo.getPluginId() + "-" + updateInfo.getVersion() + ")";
          TeamCityLog.Test test = log.testStarted(plugin);
          String pluginUrl = getPluginUrl(updateInfo);
          log.testFailed(plugin, "Plugin URL: " + pluginUrl + '\n' + "PluginId: " + updateInfo, problem.getDescription());
          test.close();
        }

        problemSuite.close();
      }

      problemTypeSuite.close();
    }

  }

  public static String convertNameToPrefix(@NotNull String className) {
    String name = className.substring(className.lastIndexOf('.') + 1);
    String[] words = name.split("(?=[A-Z])");
    if (words.length == 0) {
      return name;
    }
    if (words[words.length - 1].equals("Problem")) {
      words = Arrays.copyOf(words, words.length - 1);
    }
    return Arrays.stream(words).map(String::toLowerCase).collect(Collectors.joining(" "));
  }


  @NotNull
  private static Multimap<String, UpdateInfo> fillIdToUpdates(@NotNull Map<UpdateInfo, ProblemSet> map) {
    Multimap<String, UpdateInfo> idToUpdates = ArrayListMultimap.create();
    for (UpdateInfo updateInfo : map.keySet()) {
      String pluginId = updateInfo.getPluginId() != null ? updateInfo.getPluginId() : "#" + updateInfo.getUpdateId();
      idToUpdates.put(pluginId, updateInfo);
    }
    return idToUpdates;
  }

  public static void groupByPlugin(@NotNull TeamCityLog log, @NotNull Map<UpdateInfo, ProblemSet> map) {

    Multimap<String, UpdateInfo> idToUpdates = fillIdToUpdates(map);

    for (String pluginId : idToUpdates.keySet()) {
      List<UpdateInfo> updateInfos = ProblemUtils.sortUpdatesWithDescendingVersionsOrder(idToUpdates.get(pluginId));

      String pluginLink = REPOSITORY_PLUGIN_ID_BASE + pluginId;
      TeamCityLog.TestSuite pluginSuite = log.testSuiteStarted(pluginId);

      for (UpdateInfo updateInfo : updateInfos) {

        Map<Problem, Set<ProblemLocation>> problemToLocations = map.get(updateInfo).asMap();

        List<Problem> problems = ProblemUtils.sortProblems(problemToLocations.keySet());

        String version = updateInfo.getVersion() != null ? updateInfo.getVersion() : "#" + updateInfo.getUpdateId();
        String testName = "(" + version + (updateInfo == updateInfos.get(0) && !CheckIdeCommand.NO_COMPATIBLE_UPDATE_VERSION.equals(version) ? " - newest" : "") + ")";

        if (problems.isEmpty()) {
          //plugin has no problems => test passed.
          TeamCityLog.Test test = log.testStarted(testName);
          test.close();
        } else {

          StringBuilder builder = new StringBuilder();

          for (Problem problem : problems) {
            builder.append("#").append(problem.getDescription()).append("\n");

            for (ProblemLocation location : problemToLocations.get(problem)) {
              builder.append("      at ").append(location).append("\n");
            }
          }

          TeamCityLog.Test test = log.testStarted(testName);
          log.testStdErr(testName, builder.toString());
          log.testFailed(testName, "Plugin URL: " + pluginLink + '\n' + updateInfo + " has " + problems.size() + " " + pluralize("problem", problems.size()), "");
          test.close();

        }
      }
      pluginSuite.close();
    }
  }

  /**
   * Prints Build Problems in the Overview page or as tests
   */
  public static void printTeamCityProblems(@NotNull TeamCityLog log,
                                           @NotNull Map<UpdateInfo, ProblemSet> results,
                                           @NotNull Predicate<UpdateInfo> updateFilter,
                                           @NotNull ReportGrouping reportGrouping) {
    if (log == TeamCityLog.NULL_LOG) return;

    //list of problems without their exact problem location (only affected plugin)
    Multimap<Problem, UpdateInfo> problems = ArrayListMultimap.create();

    //fill problems map
    for (Map.Entry<UpdateInfo, ProblemSet> entry : results.entrySet()) {
      if (!updateFilter.apply(entry.getKey())) {
        continue; //this is excluded plugin
      }

      for (Problem problem : entry.getValue().getAllProblems()) {
        problems.put(problem, entry.getKey());
      }
    }

    if (reportGrouping.equals(ReportGrouping.PLUGIN_WITH_LOCATION)) {
      printReportWithLocations(log, Maps.filterKeys(results, updateFilter));
    } else {
      printReport(log, problems, reportGrouping);
    }
  }


  public enum ReportGrouping {
    PLUGIN("plugin"),
    PROBLEM_TYPE("type"),
    NONE(""),
    PLUGIN_WITH_LOCATION("plugin_with_location");

    private final String myText;

    ReportGrouping(String s) {
      myText = s;
    }

    @NotNull
    public static ReportGrouping parseGrouping(@NotNull CommandLine commandLine) {
      ReportGrouping grouping = ReportGrouping.NONE;
      String groupValue = commandLine.getOptionValue("g");
      if (groupValue != null) {
        for (ReportGrouping report : ReportGrouping.values()) {
          if (report.myText.equals(groupValue)) {
            return report;
          }
        }
        throw FailUtil.fail("Grouping argument should be one of 'plugin', 'type', 'plugin_with_location' or not set at all.");
      }
      return grouping;
    }
  }
}
