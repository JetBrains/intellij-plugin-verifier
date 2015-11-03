package com.jetbrains.pluginverifier.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.UpdateInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Sergey Evdokimov
 */
public class TeamCityUtil {

  public static void printTeamCityProblems(TeamCityLog log, Multimap<Problem, UpdateInfo> problems) {
    if (log == TeamCityLog.NULL_LOG) return;

    if (problems.isEmpty()) return;

    List<Problem> sortedProblems = ProblemUtils.sort(problems.keySet());

    for (Problem problem : sortedProblems) {
      List<UpdateInfo> updates = new ArrayList<UpdateInfo>(problems.get(problem));

      ProblemUtils.sortUpdates(updates);

      log.buildProblem(MessageUtils.cutCommonPackages(problem.getDescription()) + " (in " + Joiner.on(", ").join(updates) + ')');
    }
  }

}
