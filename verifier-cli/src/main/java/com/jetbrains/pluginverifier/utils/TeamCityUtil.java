package com.jetbrains.pluginverifier.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.jetbrains.pluginverifier.problems.FailUtil;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.UpdateInfo;
import org.apache.commons.cli.CommandLine;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
public class TeamCityUtil {

  public static void printTeamCityProblems(TeamCityLog log, Multimap<Problem, UpdateInfo> problems) {
    if (log == TeamCityLog.NULL_LOG) return;
    if (problems.isEmpty()) return;

    List<Problem> sortedProblems = ProblemUtils.sortProblems(problems.keySet());

    for (Problem problem : sortedProblems) {
      List<UpdateInfo> updates = new ArrayList<UpdateInfo>(problems.get(problem));

      ProblemUtils.sortUpdates(updates);

      log.buildProblem(MessageUtils.cutCommonPackages(problem.getDescription()) + " (in " + Joiner.on(", ").join(updates) + ')');
    }
  }

  public static void printGroupedReport(@NotNull TeamCityLog log,
                                        @NotNull Multimap<Problem, UpdateInfo> problems,
                                        @NotNull ReportGrouping reportGrouping) {
    if (log == TeamCityLog.NULL_LOG) return;
    if (problems.isEmpty()) return;



    //TODO
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
