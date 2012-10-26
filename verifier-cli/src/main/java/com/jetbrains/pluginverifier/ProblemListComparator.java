package com.jetbrains.pluginverifier;

import com.jetbrains.pluginverifier.utils.ProblemUtils;
import com.jetbrains.pluginverifier.util.Util;
import com.jetbrains.pluginverifier.problems.Problem;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author Sergey Evdokimov
 */
public class ProblemListComparator {

  public static void compare(@NotNull String currentBuild, @NotNull String previousBuild) throws IOException {
    File home = Util.getValidatorHome();

    File currentBuildProblems = new File(home, currentBuild + ".xml");
    File previousBuildProblems = new File(home, previousBuild + ".xml");

    Map<Integer, Set<Problem>> currentProblemMap = ProblemUtils.loadProblems(currentBuildProblems);
    Map<Integer, Set<Problem>> previousProblemMap = ProblemUtils.loadProblems(previousBuildProblems);

    System.out.println("New problems:");

    for (Map.Entry<Integer, Set<Problem>> entry : currentProblemMap.entrySet()) {
      Set<Problem> oldProblemSet = previousProblemMap.get(entry.getKey());

      if (oldProblemSet == null) continue;

      boolean isFirst = true;

      Set<Problem> currentProblemSet = entry.getValue();

      for (Problem problem : currentProblemSet) {
        if (!oldProblemSet.contains(problem)) {
          if (isFirst) {
            System.out.println("Update #" + entry.getKey());
            isFirst = false;
          }

          System.out.println(problem.getDescription());
        }
      }
    }

    System.out.println("Done");
  }

}
