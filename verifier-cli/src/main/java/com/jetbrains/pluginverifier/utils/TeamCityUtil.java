package com.jetbrains.pluginverifier.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.jetbrains.pluginverifier.problems.Problem;
import com.jetbrains.pluginverifier.problems.UpdateInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
      Collections.sort(updates, new Comparator<UpdateInfo>() {
        @Override
        public int compare(UpdateInfo o1, UpdateInfo o2) {
          //in DESCENDING order of versions
          return VersionComparatorUtil.compare(o2.getVersion(), o1.getVersion());
        }
      });

      log.buildProblem(MessageUtils.cutCommonPackages(problem.getDescription()) + " (in " + Joiner.on(", ").join(updates) + ')');
    }
  }

}
